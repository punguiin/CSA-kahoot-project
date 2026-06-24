import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';

const MOCK_QUESTION = {
    id: 1,
    text: "Який протокол використовується для безпечного передавання гіпертексту?",
    answers: ["HTTP", "FTP", "HTTPS", "TCP/IP"],
    timeLimit: 15
};

const MOCK_LEADERBOARD = [
    { rank: 1, username: "nazar", score: 4500 },
    { rank: 2, username: "nikita", score: 4250 },
    { rank: 3, username: "student_123", score: 3800 }
];

const SHAPES = [
    <svg viewBox="0 0 24 24" fill="currentColor" className="w-10 h-10 text-white drop-shadow-md"><path d="M12 2L22 20H2Z" /></svg>,
    <svg viewBox="0 0 24 24" fill="currentColor" className="w-10 h-10 text-white drop-shadow-md"><path d="M12 2L22 12L12 22L2 12Z" /></svg>,
    <svg viewBox="0 0 24 24" fill="currentColor" className="w-10 h-10 text-white drop-shadow-md"><path d="M12 22C17.5228 22 22 17.5228 22 12C22 6.47715 17.5228 2 12 2C6.47715 2 2 6.47715 2 12C2 17.5228 6.47715 22 12 22Z" /></svg>,
    <svg viewBox="0 0 24 24" fill="currentColor" className="w-10 h-10 text-white drop-shadow-md"><path d="M3 3H21V21H3V3Z" /></svg>
];

const COLORS = [
    "bg-red-500", "bg-blue-500", "bg-yellow-500", "bg-green-500"
];

const HostView = () => {
    const { pin } = useParams();
    const navigate = useNavigate();
    const [phase, setPhase] = useState<'LOBBY' | 'QUESTION' | 'LEADERBOARD'>('LOBBY');
    const [timeLeft, setTimeLeft] = useState(MOCK_QUESTION.timeLimit);
    const [players] = useState<string[]>(['Микола', 'Назар', 'Нікіта']);
    const [answersCount, setAnswersCount] = useState(0);

    useEffect(() => {
        if (phase === 'QUESTION') {
            if (timeLeft > 0) {
                const timerId = setTimeout(() => {
                    setTimeLeft(timeLeft - 1);
                    if (timeLeft % 4 === 0 && answersCount < players.length) {
                        setAnswersCount(prev => prev + 1);
                    }
                }, 1000);
                return () => clearTimeout(timerId);
            } else {
                setPhase('LEADERBOARD');
            }
        }
    }, [timeLeft, phase, answersCount, players.length]);

    const startGame = () => {
        setPhase('QUESTION');
    };

    const skipTimer = () => {
        setTimeLeft(0);
    };

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
                        className="bg-green-500 text-white px-10 py-4 rounded-full font-black text-2xl shadow-xl hover:bg-green-400 hover:scale-105 transition-all"
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

    if (phase === 'LEADERBOARD') {
        return (
            <div className="min-h-screen bg-blue-600 flex flex-col items-center justify-center p-8">
                <div className="w-full max-w-4xl bg-white rounded-3xl shadow-2xl overflow-hidden flex flex-col">
                    <div className="bg-gray-900 text-white p-8 text-center flex justify-between items-center">
                        <h2 className="text-4xl font-bold">Таблиця лідерів</h2>
                        <button
                            onClick={() => navigate('/dashboard')}
                            className="bg-blue-600 text-white px-8 py-3 rounded-lg font-bold text-xl hover:bg-blue-700 transition-colors"
                        >
                            Завершити гру
                        </button>
                    </div>

                    <div className="p-10 flex flex-col gap-6">
                        {MOCK_LEADERBOARD.map((player, index) => (
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
                                    <span className="w-12 text-center">{player.rank}.</span>
                                    <span>@{player.username}</span>
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

                    {/* Блок таймера з кнопкою пропуску */}
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
                        <span className="text-gray-500 font-bold uppercase tracking-widest mb-2 text-sm">Відповіло</span>
                        <span className="text-6xl font-black text-gray-800">{answersCount} <span className="text-3xl text-gray-400">/ {players.length}</span></span>
                    </div>

                    <h1 className="text-5xl font-bold text-center text-gray-800 max-w-3xl leading-tight">
                        {MOCK_QUESTION.text}
                    </h1>
                </div>

                <div className="grid grid-cols-2 gap-4 h-64">
                    {MOCK_QUESTION.answers.map((answer, index) => (
                        <div key={index} className={`flex items-center p-8 rounded-2xl ${COLORS[index]} shadow-md`}>
                            <div className="w-16 flex justify-center shrink-0">
                                {SHAPES[index]}
                            </div>
                            <span className="text-white text-4xl font-bold ml-6 text-left drop-shadow-md">
                                {answer}
                            </span>
                        </div>
                    ))}
                </div>
            </main>
        </div>
    );
};

export default HostView;