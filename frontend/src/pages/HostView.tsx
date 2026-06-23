import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { gameClient, MessageType } from '../net/gameClient';

interface BoardEntry { nickname: string; score: number; }
interface RosterPlayer { nickname: string; score: number; }

type Phase = 'LOBBY' | 'RUNNING' | 'FINISHED';

const HostView = () => {
    const { pin } = useParams();
    const navigate = useNavigate();

    const [phase, setPhase] = useState<Phase>('LOBBY');
    const [players, setPlayers] = useState<string[]>([]);
    const [leaderboard, setLeaderboard] = useState<BoardEntry[]>([]);
    const [starting, setStarting] = useState(false);
    const [closed, setClosed] = useState(false);

    useEffect(() => {
        const roster = (p: any) => setPlayers((p.players as RosterPlayer[]).map((x) => x.nickname));
        const offJoined = gameClient.on(MessageType.PLAYER_JOINED, roster);
        const offLeft = gameClient.on(MessageType.PLAYER_LEFT, roster);
        const offQuestion = gameClient.on(MessageType.QUESTION, () => {
            setStarting(false);
            setPhase((prev) => (prev === 'LOBBY' ? 'RUNNING' : prev));
        });
        const offLeaderboard = gameClient.on(MessageType.LEADERBOARD, (p: any) => {
            setLeaderboard(p.leaderboard);
            if (p.state === 'FINISHED') setPhase('FINISHED');
            else if (p.state && p.state !== 'LOBBY') setPhase((prev) => (prev === 'LOBBY' ? 'RUNNING' : prev));
        });
        const offFinished = gameClient.on(MessageType.GAME_FINISHED, (p: any) => {
            setLeaderboard(p.leaderboard);
            setPhase('FINISHED');
        });
        const offClosed = gameClient.on(MessageType.ROOM_CLOSED, () => {
            gameClient.clearSession();
            setClosed(true);
        });
        return () => {
            offJoined();
            offLeft();
            offQuestion();
            offLeaderboard();
            offFinished();
            offClosed();
        };
    }, []);

    useEffect(() => {
        if (!gameClient.isOpen()) {
            if (gameClient.session?.role === 'HOST') {
                gameClient.resume().catch(() => navigate('/dashboard'));
            } else {
                navigate('/dashboard');
            }
        }

    }, []);

    useEffect(() => {
        if (phase !== 'RUNNING') return;
        gameClient.send(MessageType.REQ_GET_LEADERBOARD, {});
        const id = setInterval(() => gameClient.send(MessageType.REQ_GET_LEADERBOARD, {}), 3000);
        return () => clearInterval(id);
    }, [phase]);

    const handleStart = () => {
        if (players.length === 0) return;
        setStarting(true);
        gameClient.send(MessageType.REQ_START_QUIZ, {});
    };

    const leaveToDashboard = () => {
        gameClient.clearSession();
        navigate('/dashboard');
    };

    if (closed) {
        return (
            <div className="min-h-screen bg-gray-900 flex flex-col items-center justify-center p-4 text-center">
                <div className="bg-white p-8 rounded-3xl shadow-2xl max-w-md w-full">
                    <h2 className="text-3xl font-black text-gray-800 mb-4">Сесію завершено</h2>
                    <p className="text-gray-600 mb-8 font-medium">Цю ігрову сесію було закрито.</p>
                    <button onClick={() => navigate('/dashboard')} className="w-full bg-gray-100 text-gray-800 px-6 py-4 rounded-xl font-bold hover:bg-gray-200 transition-colors">
                        До панелі керування
                    </button>
                </div>
            </div>
        );
    }

    if (phase === 'FINISHED') {
        return (
            <div className="min-h-screen bg-blue-600 flex flex-col items-center justify-center p-4">
                <h1 className="text-white text-5xl font-black mb-12 drop-shadow-lg text-center tracking-wide">
                    Переможці:
                </h1>
                <div className="w-full max-w-2xl bg-white rounded-2xl shadow-2xl p-8 flex flex-col gap-4 mb-8">
                    {leaderboard.map((player, index) => (
                        <div
                            key={index}
                            className={`flex items-center justify-between p-4 rounded-xl font-bold text-lg ${
                                index === 0 ? 'bg-yellow-100 text-yellow-800 border-2 border-yellow-400' :
                                    index === 1 ? 'bg-gray-100 text-gray-700 border-2 border-gray-300' :
                                        index === 2 ? 'bg-orange-50 text-orange-800 border-2 border-orange-300' :
                                            'bg-white text-gray-700 border border-gray-200'
                            }`}
                        >
                            <div className="flex items-center gap-4">
                                <span className="w-8 text-center">{index + 1}.</span>
                                <span>@{player.nickname}</span>
                            </div>
                            <span>{player.score}</span>
                        </div>
                    ))}
                </div>
                <button onClick={leaveToDashboard} className="bg-white text-gray-800 px-8 py-3 rounded-lg font-bold hover:bg-gray-100 transition-colors">
                    До панелі керування
                </button>
            </div>
        );
    }

    if (phase === 'RUNNING') {
        return (
            <div className="min-h-screen bg-blue-600 flex flex-col items-center p-8">
                <div className="w-full max-w-3xl flex justify-between items-center mb-10">
                    <h2 className="text-white text-3xl font-bold drop-shadow-md">PIN: {pin}</h2>
                    <div className="bg-black/20 px-6 py-2 rounded-full text-white font-bold text-xl">
                        Гравців: {players.length}
                    </div>
                </div>

                <h1 className="text-white text-4xl font-bold mb-10 text-center drop-shadow-lg">Гра триває…</h1>

                <div className="w-full max-w-2xl bg-white rounded-2xl shadow-2xl overflow-hidden flex flex-col">
                    <div className="bg-gray-900 text-white p-6 text-center">
                        <h2 className="text-2xl font-bold">Таблиця лідерів</h2>
                    </div>
                    <div className="p-6 flex flex-col gap-3">
                        {leaderboard.length === 0 && (
                            <div className="text-center text-gray-400 font-medium py-4">Очікуємо перші відповіді…</div>
                        )}
                        {leaderboard.map((player, index) => (
                            <div key={index} className="flex items-center justify-between p-4 rounded-xl font-bold text-lg bg-gray-50 text-gray-700 border border-gray-200">
                                <div className="flex items-center gap-4">
                                    <span className="w-8 text-center">{index + 1}.</span>
                                    <span>@{player.nickname}</span>
                                </div>
                                <span>{player.score}</span>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-blue-600 flex flex-col items-center p-8 relative">
            <div className="w-full flex justify-between items-center mb-8">
                <button onClick={leaveToDashboard} className="text-white/80 hover:text-white font-bold transition-colors">
                    ← Вийти
                </button>
                <div className="bg-black/20 px-6 py-2 rounded-full text-white font-bold text-xl">
                    Гравців: {players.length}
                </div>
            </div>

            <p className="text-white/80 font-bold uppercase tracking-widest mb-2">Приєднуйтесь за PIN-кодом</p>
            <div className="bg-white px-10 py-5 rounded-2xl shadow-2xl mb-10">
                <span className="text-6xl font-black text-gray-900 tracking-[0.2em]">{pin}</span>
            </div>

            <button
                onClick={handleStart}
                disabled={players.length === 0 || starting}
                className="bg-green-500 text-white text-2xl font-black px-12 py-4 rounded-xl shadow-lg hover:bg-green-600 transition-colors disabled:bg-gray-400 disabled:cursor-not-allowed mb-12"
            >
                {starting ? 'Запуск…' : 'Розпочати гру'}
            </button>

            <div className="flex flex-wrap justify-center gap-4 w-full max-w-4xl">
                {players.length === 0 && (
                    <p className="text-white/80 text-xl font-medium animate-pulse">Очікуємо гравців…</p>
                )}
                {players.map((player, index) => (
                    <div key={index} className="bg-white/20 px-6 py-3 rounded-lg text-white text-2xl font-bold shadow-sm">
                        {player}
                    </div>
                ))}
            </div>
        </div>
    );
};

export default HostView;
