import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { gameClient, MessageType } from '../net/gameClient';

interface WireQuestion {
    pin: string;
    index: number;
    text: string;
    timeLimit: number;
    answers: { id: number; text: string }[];
}

interface BoardEntry { nickname: string; score: number; }

const SHAPES = [
    <svg viewBox="0 0 24 24" fill="currentColor" className="w-10 h-10 text-white drop-shadow-md"><path d="M12 2L22 20H2Z" /></svg>,
    <svg viewBox="0 0 24 24" fill="currentColor" className="w-10 h-10 text-white drop-shadow-md"><path d="M12 2L22 12L12 22L2 12Z" /></svg>,
    <svg viewBox="0 0 24 24" fill="currentColor" className="w-10 h-10 text-white drop-shadow-md"><path d="M12 22C17.5228 22 22 17.5228 22 12C22 6.47715 17.5228 2 12 2C6.47715 2 2 6.47715 2 12C2 17.5228 6.47715 22 12 22Z" /></svg>,
    <svg viewBox="0 0 24 24" fill="currentColor" className="w-10 h-10 text-white drop-shadow-md"><path d="M3 3H21V21H3V3Z" /></svg>
];

const COLORS = ["bg-red-500", "bg-blue-500", "bg-yellow-500", "bg-green-500"];

const HostView = () => {
    const { pin } = useParams();
    const navigate = useNavigate();
    const [phase, setPhase] = useState<'LOBBY' | 'QUESTION' | 'LEADERBOARD' | 'FINISHED'>('LOBBY');
    const [players, setPlayers] = useState<string[]>([]);
    const [question, setQuestion] = useState<WireQuestion | null>(null);
    const [leaderboard, setLeaderboard] = useState<BoardEntry[]>([]);
    const [timeLeft, setTimeLeft] = useState(0);

    useEffect(() => {
        const roster = (p: any) => setPlayers((p.players as BoardEntry[]).map((x) => x.nickname));
        const offJoined = gameClient.on(MessageType.PLAYER_JOINED, roster);
        const offLeft = gameClient.on(MessageType.PLAYER_LEFT, roster);
        const offQuestion = gameClient.on(MessageType.QUESTION, (q: WireQuestion) => {
            setQuestion(q);
            setTimeLeft(q.timeLimit);
            setPhase('QUESTION');
        });
        const offLeaderboard = gameClient.on(MessageType.LEADERBOARD, (p: any) => {
            setLeaderboard(p.leaderboard);
            // GET_LEADERBOARD reply during the lobby just seeds the roster; only a real
            // round end (state === LEADERBOARD) advances the host screen.
            setPlayers((p.leaderboard as BoardEntry[]).map((x) => x.nickname));
            if (p.state === 'LEADERBOARD') {
                setPhase('LEADERBOARD');
            }
        });
        const offFinished = gameClient.on(MessageType.GAME_FINISHED, (p: any) => {
            setLeaderboard(p.leaderboard);
            setPhase('FINISHED');
        });

        // Seed the current roster in case players joined before this screen mounted.
        gameClient.send(MessageType.REQ_GET_LEADERBOARD, {});

        return () => {
            offJoined();
            offLeft();
            offQuestion();
            offLeaderboard();
            offFinished();
        };
    }, []);

    // Host owns the clock: when it runs out, end the round and let the server broadcast standings.
    useEffect(() => {
        if (phase !== 'QUESTION') return;
        if (timeLeft > 0) {
            const id = setTimeout(() => setTimeLeft(timeLeft - 1), 1000);
            return () => clearTimeout(id);
        }
        gameClient.send(MessageType.REQ_END_ROUND, {});
    }, [phase, timeLeft]);

    const startGame = () => gameClient.send(MessageType.REQ_START_QUIZ, {});
    const skipTimer = () => setTimeLeft(0);
    const nextQuestion = () => gameClient.send(MessageType.REQ_NEXT_QUESTION, {});

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

    if (phase === 'LEADERBOARD' || phase === 'FINISHED') {
        const finished = phase === 'FINISHED';
        return (
            <div className="min-h-screen bg-blue-600 flex flex-col items-center justify-center p-8">
                <div className="w-full max-w-4xl bg-white rounded-3xl shadow-2xl overflow-hidden flex flex-col">
                    <div className="bg-gray-900 text-white p-8 text-center flex justify-between items-center">
                        <h2 className="text-4xl font-bold">{finished ? 'Підсумки гри' : 'Таблиця лідерів'}</h2>
                        {finished ? (
                            <button
                                onClick={() => navigate('/dashboard')}
                                className="bg-blue-600 text-white px-8 py-3 rounded-lg font-bold text-xl hover:bg-blue-700 transition-colors"
                            >
                                Завершити гру
                            </button>
                        ) : (
                            <button
                                onClick={nextQuestion}
                                className="bg-green-500 text-white px-8 py-3 rounded-lg font-bold text-xl hover:bg-green-400 transition-colors"
                            >
                                Далі
                            </button>
                        )}
                    </div>

                    <div className="p-10 flex flex-col gap-6">
                        {leaderboard.map((player, index) => (
                            <div
                                key={index}
                                className={`flex items-center justify-between p-6 rounded-2xl font-bold text-3xl ${
                                    index === 0 ? 'bg-yellow-100 text-yellow-800 border-2 border-yellow-400' :
                                        index === 1 ? 'bg-gray-100 text-gray-700 border-2 border-gray-300' :
                                            index === 2 ? 'bg-orange-50 text-orange-800 border-2 border-orange-300' :
                                                'bg-white text-gray-700 border border-gray-200'
                                }`}
                            >
                                <div className="flex items-center gap-6">
                                    <span className="w-12 text-center">{index + 1}.</span>
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
        <div className="min-h-screen bg-gray-100 flex flex-col">
            <header className="bg-white shadow-sm p-4 flex justify-between items-center z-10 ">
                <div className="text-2xl font-black text-blue-600 tracking-tighter flex items-center gap-3">
                    KMAhoot!
                    <span className="bg-gray-800 text-white text-xs font-bold px-3 py-1 rounded-full uppercase tracking-wider">
                        Екран Ведучого
                    </span>
                </div>
                <div className="flex items-center gap-4">
                    <button
                        onClick={() => navigate('/dashboard')}
                        className="text-red-500 font-bold text-sm hover:underline"
                    >
                        Завершити сесію
                    </button>
                    <div className="bg-gray-900 text-white px-6 py-2 rounded-full font-bold text-xl">
                        PIN: {pin}
                    </div>
                </div>
            </header>

            <main className="flex-1 flex flex-col p-8 max-w-7xl mx-auto w-full gap-8">
                <div className="flex-1 flex flex-col items-center justify-center relative bg-white rounded-3xl shadow-md border border-gray-200 p-12">
                    <div className="absolute left-10 top-1/2 -translate-y-1/2 flex flex-col items-center gap-3">
                        <div className="w-32 h-32 bg-purple-600 rounded-full flex flex-col items-center justify-center shadow-xl border-8 border-purple-200">
                            <span className="text-6xl font-black text-white">{timeLeft}</span>
                        </div>
                        <button
                            onClick={skipTimer}
                            className="bg-gray-100 hover:bg-gray-200 text-gray-700 font-bold px-4 py-2 rounded-lg text-sm transition-colors border border-gray-300"
                        >
                            Пропустити
                        </button>
                    </div>

                    <div className="absolute right-10 top-1/2 -translate-y-1/2 flex flex-col items-center justify-center">
                        <span className="text-gray-500 font-bold uppercase tracking-widest mb-2 text-sm">Гравців</span>
                        <span className="text-6xl font-black text-gray-800">{players.length}</span>
                    </div>

                    <h1 className="text-5xl font-bold text-center text-gray-800 max-w-3xl leading-tight">
                        {question?.text}
                    </h1>
                </div>

                <div className="grid grid-cols-2 gap-4 h-64">
                    {question?.answers.map((answer, index) => (
                        <div key={answer.id} className={`flex items-center p-8 rounded-2xl ${COLORS[index % COLORS.length]} shadow-md`}>
                            <div className="w-16 flex justify-center shrink-0">
                                {SHAPES[index % SHAPES.length]}
                            </div>
                            <span className="text-white text-4xl font-bold ml-6 text-left drop-shadow-md">
                                {answer.text}
                            </span>
                        </div>
                    ))}
                </div>
            </main>
        </div>
    );
};

export default HostView;
