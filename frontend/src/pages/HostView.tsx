import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { gameClient, MessageType } from '../net/gameClient';

interface BoardEntry { nickname: string; score: number; }

const HostView = () => {
    const { pin } = useParams();
    const navigate = useNavigate();
    const [phase, setPhase] = useState<'LOBBY' | 'RUNNING' | 'FINISHED'>('LOBBY');
    const [players, setPlayers] = useState<string[]>([]);
    const [leaderboard, setLeaderboard] = useState<BoardEntry[]>([]);

    useEffect(() => {
        const roster = (p: any) => setPlayers((p.players as BoardEntry[]).map((x) => x.nickname));
        const offJoined = gameClient.on(MessageType.PLAYER_JOINED, roster);
        const offLeft = gameClient.on(MessageType.PLAYER_LEFT, roster);

        const offQuestion = gameClient.on(MessageType.QUESTION, () => setPhase((ph) => (ph === 'LOBBY' ? 'RUNNING' : ph)));
        const offLeaderboard = gameClient.on(MessageType.LEADERBOARD, (p: any) => {
            setLeaderboard(p.leaderboard);
            setPlayers((p.leaderboard as BoardEntry[]).map((x) => x.nickname));
            if (p.state === 'FINISHED') {
                setPhase('FINISHED');
            } else if (p.state !== 'LOBBY') {
                setPhase('RUNNING');
            }
        });
        const offClosed = gameClient.on(MessageType.ROOM_CLOSED, () => {
            gameClient.clearSession();
            navigate('/dashboard');
        });
        return () => {
            offJoined();
            offLeft();
            offQuestion();
            offLeaderboard();
            offClosed();
        };

    }, []);

    useEffect(() => {
        const init = async () => {
            if (!gameClient.isOpen()) {
                if (gameClient.session?.role === 'HOST') {
                    try {
                        await gameClient.resume();
                    } catch {
                        navigate('/dashboard');
                        return;
                    }
                } else {
                    navigate('/dashboard');
                    return;
                }
            }
            gameClient.send(MessageType.REQ_GET_LEADERBOARD, {});
        };
        init();

    }, []);

    useEffect(() => {
        if (phase !== 'RUNNING') return;
        const id = setInterval(() => gameClient.send(MessageType.REQ_GET_LEADERBOARD, {}), 1500);
        return () => clearInterval(id);
    }, [phase]);

    const startGame = () => gameClient.send(MessageType.REQ_START_QUIZ, {});
    const finishGame = () => {
        gameClient.clearSession();
        navigate('/dashboard');
    };

    const board = (
        <div className="flex flex-col gap-4">
            {leaderboard.length === 0 && (
                <div className="text-center text-gray-400 font-medium py-6">Поки що немає рахунку</div>
            )}
            {leaderboard.map((player, index) => (
                <div
                    key={index}
                    className={`flex items-center justify-between p-5 rounded-2xl font-bold text-2xl ${
                        index === 0 ? 'bg-yellow-100 text-yellow-800 border-2 border-yellow-400' :
                            index === 1 ? 'bg-gray-100 text-gray-700 border-2 border-gray-300' :
                                index === 2 ? 'bg-orange-50 text-orange-800 border-2 border-orange-300' :
                                    'bg-white text-gray-700 border border-gray-200'
                    }`}
                >
                    <div className="flex items-center gap-5">
                        <span className="w-10 text-center">{index + 1}.</span>
                        <span>@{player.nickname}</span>
                    </div>
                    <span>{player.score}</span>
                </div>
            ))}
        </div>
    );

    if (phase === 'LOBBY') {
        return (
            <div className="min-h-screen bg-gray-100 flex flex-col items-center justify-center p-8 relative">
                <div className="bg-white p-12 rounded-3xl shadow-2xl text-center max-w-2xl w-full border border-gray-200 mb-12">
                    <h2 className="text-gray-500 font-bold text-2xl mb-4 uppercase tracking-widest">Приєднуйтесь за PIN-кодом</h2>
                    <div className="text-8xl font-black text-gray-900 tracking-tighter drop-shadow-sm">
                        {pin}
                    </div>
                </div>

                <div className="w-full flex justify-between items-center px-12 mb-8">
                    <div className="bg-blue-600 px-8 py-3 rounded-full text-white font-bold text-2xl shadow-lg">
                        Гравців: {players.length}
                    </div>
                    <button
                        onClick={startGame}
                        disabled={players.length === 0}
                        className="bg-green-500 text-white px-10 py-4 rounded-full font-black text-2xl shadow-xl hover:bg-green-400 hover:scale-105 transition-all disabled:bg-gray-300 disabled:scale-100"
                    >
                        Почати гру
                    </button>
                </div>

                <div className="flex flex-wrap justify-center gap-4 w-full max-w-5xl">
                    {players.map((player, index) => (
                        <div key={index} className="bg-white px-8 py-4 rounded-xl text-gray-800 text-3xl font-bold shadow-md border border-gray-200">
                            {player}
                        </div>
                    ))}
                </div>
            </div>
        );
    }

    const finished = phase === 'FINISHED';
    return (
        <div className="min-h-screen bg-blue-600 flex flex-col items-center justify-center p-8">
            <div className="w-full max-w-4xl bg-white rounded-3xl shadow-2xl overflow-hidden flex flex-col">
                <div className="bg-gray-900 text-white p-8 flex justify-between items-center">
                    <div>
                        <h2 className="text-4xl font-bold">{finished ? 'Підсумки гри' : 'Таблиця лідерів'}</h2>
                        <p className="text-gray-400 mt-1 font-medium">
                            {finished ? 'Усі гравці завершили' : 'Гравці проходять тест у власному темпі…'}
                        </p>
                    </div>
                    <div className="flex items-center gap-4">
                        <div className="bg-gray-700 text-white px-5 py-2 rounded-full font-bold text-lg">PIN: {pin}</div>
                        <button
                            onClick={finishGame}
                            className="bg-blue-600 text-white px-8 py-3 rounded-lg font-bold text-xl hover:bg-blue-700 transition-colors"
                        >
                            Завершити гру
                        </button>
                    </div>
                </div>
                <div className="p-10">{board}</div>
            </div>
        </div>
    );
};

export default HostView;
