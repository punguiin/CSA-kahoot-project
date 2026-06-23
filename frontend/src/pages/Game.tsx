import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
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
    <svg viewBox="0 0 24 24" fill="currentColor" className="w-8 h-8 text-white drop-shadow-md"><path d="M12 2L22 20H2Z" /></svg>,
    <svg viewBox="0 0 24 24" fill="currentColor" className="w-8 h-8 text-white drop-shadow-md"><path d="M12 2L22 12L12 22L2 12Z" /></svg>,
    <svg viewBox="0 0 24 24" fill="currentColor" className="w-8 h-8 text-white drop-shadow-md"><path d="M12 22C17.5228 22 22 17.5228 22 12C22 6.47715 17.5228 2 12 2C6.47715 2 2 6.47715 2 12C2 17.5228 6.47715 22 12 22Z" /></svg>,
    <svg viewBox="0 0 24 24" fill="currentColor" className="w-8 h-8 text-white drop-shadow-md"><path d="M3 3H21V21H3V3Z" /></svg>
];

const COLORS = [
    "bg-red-500 hover:bg-red-600 border-red-700",
    "bg-blue-500 hover:bg-blue-600 border-blue-700",
    "bg-yellow-500 hover:bg-yellow-600 border-yellow-600",
    "bg-green-500 hover:bg-green-600 border-green-700"
];

const Game = () => {
    const navigate = useNavigate();
    const [question, setQuestion] = useState<WireQuestion | null>(gameClient.lastQuestion);
    const [timeLeft, setTimeLeft] = useState<number>(gameClient.lastQuestion?.timeLimit ?? 0);
    const [selectedAnswer, setSelectedAnswer] = useState<number | null>(null);
    const [view, setView] = useState<'QUESTION' | 'LEADERBOARD' | 'PODIUM'>('QUESTION');
    const [leaderboard, setLeaderboard] = useState<BoardEntry[]>([]);
    const [errorMsg, setErrorMsg] = useState('');
    const [closed, setClosed] = useState(false);

    useEffect(() => {
        const offQuestion = gameClient.on(MessageType.QUESTION, (q: WireQuestion) => {
            setQuestion(q);
            setSelectedAnswer(null);
            setTimeLeft(q.timeLimit);
            setView('QUESTION');
        });
        const offResult = gameClient.on(MessageType.ANSWER_RESULT, () => {

        });
        const offLeaderboard = gameClient.on(MessageType.LEADERBOARD, (p: any) => {
            setLeaderboard(p.leaderboard);
            setView('LEADERBOARD');
        });
        const offFinished = gameClient.on(MessageType.GAME_FINISHED, (p: any) => {
            setLeaderboard(p.leaderboard);
            setView('PODIUM');
        });
        const offClosed = gameClient.on(MessageType.ROOM_CLOSED, () => {
            gameClient.clearSession();
            setClosed(true);
        });
        return () => {
            offQuestion();
            offResult();
            offLeaderboard();
            offFinished();
            offClosed();
        };
    }, []);

    useEffect(() => {
        if (!gameClient.isOpen()) {
            if (gameClient.session?.role === 'PLAYER') {
                gameClient.resume().catch(() => navigate('/'));
            } else {
                navigate('/');
            }
        }

    }, []);

    useEffect(() => {
        if (view !== 'QUESTION' || timeLeft <= 0) return;
        const id = setTimeout(() => setTimeLeft(timeLeft - 1), 1000);
        return () => clearTimeout(id);
    }, [view, timeLeft]);

    const handleAnswerClick = (index: number) => {
        if (selectedAnswer !== null) {
            setErrorMsg('Ви вже надіслали відповідь на це запитання!');
            setTimeout(() => setErrorMsg(''), 3000);
            return;
        }
        if (!question) return;
        setSelectedAnswer(index);
        gameClient.send(MessageType.REQ_SUBMIT_ANSWER, { answerId: question.answers[index].id });
    };

    if (closed) {
        return (
            <div className="min-h-screen bg-gray-900 flex flex-col items-center justify-center p-4 text-center">
                <div className="bg-white p-8 rounded-3xl shadow-2xl max-w-md w-full">
                    <h2 className="text-3xl font-black text-gray-800 mb-4">Гру завершено</h2>
                    <p className="text-gray-600 mb-8 font-medium">Адміністратор примусово завершив цю ігрову сесію.</p>
                    <button onClick={() => navigate('/')} className="w-full bg-gray-100 text-gray-800 px-6 py-4 rounded-xl font-bold hover:bg-gray-200 transition-colors">
                        Повернутися на головну
                    </button>
                </div>
            </div>
        );
    }

    if (!question) {
        return (
            <div className="min-h-screen bg-blue-600 flex flex-col items-center justify-center p-4 text-white">
                <h1 className="text-3xl font-bold mb-6">Очікуємо початку гри...</h1>
                <button onClick={() => { gameClient.clearSession(); navigate('/'); }} className="bg-white/20 px-6 py-3 rounded-lg font-bold">
                    На головну
                </button>
            </div>
        );
    }

    if (view === 'PODIUM') {
        const top = leaderboard;
        return (
            <div className="min-h-screen bg-blue-600 flex flex-col items-center justify-center p-4">
                <h1 className="text-white text-5xl font-black mb-12 drop-shadow-lg text-center tracking-wide">
                    Переможці:
                </h1>
                <div className="w-full max-w-2xl bg-white rounded-2xl shadow-2xl p-8 flex flex-col gap-4 mb-8">
                    {top.map((player, index) => (
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
                <button onClick={() => { gameClient.clearSession(); navigate('/'); }} className="bg-white text-gray-800 px-8 py-3 rounded-lg font-bold hover:bg-gray-100 transition-colors">
                    На головну
                </button>
            </div>
        );
    }

    if (view === 'LEADERBOARD') {
        return (
            <div className="min-h-screen bg-blue-600 flex flex-col items-center justify-center p-4">
                <div className="w-full max-w-2xl bg-white rounded-2xl shadow-2xl overflow-hidden flex flex-col">
                    <div className="bg-gray-900 text-white p-6 text-center">
                        <h2 className="text-3xl font-bold">Таблиця лідерів</h2>
                        <p className="text-gray-400 mt-2">Раунд завершено</p>
                    </div>
                    <div className="p-8 flex flex-col gap-4">
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
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-gray-100 flex flex-col relative">
            <header className="bg-white shadow-sm p-4 flex justify-between items-center z-10">
                <div className="text-xl md:text-2xl font-black text-blue-600 tracking-tighter">KMAhoot!</div>
                <div className="text-gray-500 font-bold text-sm md:text-base">Запитання {question.index + 1}</div>
                <div className="bg-gray-900 text-white px-4 py-1.5 rounded-full font-bold text-sm md:text-base">
                    PIN: {question.pin}
                </div>
            </header>

            {errorMsg && (
                <div className="fixed top-20 left-1/2 transform -translate-x-1/2 bg-red-500 text-white px-6 py-3 rounded-xl shadow-xl font-bold z-50 animate-bounce text-sm">
                    {errorMsg}
                </div>
            )}

            <main className="flex-1 flex flex-col p-4 md:p-6 max-w-5xl mx-auto w-full gap-6">
                <div className="flex-1 flex items-center justify-center relative bg-white rounded-2xl shadow-sm border border-gray-200 p-6 md:p-10">
                    <div className="absolute left-6 top-1/2 -translate-y-1/2 w-16 h-16 bg-purple-600 rounded-full flex items-center justify-center shadow-lg border-4 border-purple-200 hidden md:flex">
                        <span className="text-2xl font-black text-white">{timeLeft}</span>
                    </div>

                    <div className="absolute top-4 left-4 bg-purple-600 text-white px-3 py-1 rounded-full font-bold text-sm md:hidden">
                        Час: {timeLeft}
                    </div>

                    <h1 className="text-xl md:text-3xl font-bold text-center text-gray-800 max-w-3xl leading-snug">
                        {question.text}
                    </h1>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 h-auto md:h-48">
                    {question.answers.map((answer, index) => {
                        const isSelected = selectedAnswer === index;
                        const isOtherSelected = selectedAnswer !== null && selectedAnswer !== index;

                        return (
                            <button
                                key={answer.id}
                                onClick={() => handleAnswerClick(index)}
                                disabled={selectedAnswer !== null}
                                className={`
                                    relative flex items-center p-4 rounded-xl border-b-[6px] transition-all duration-200
                                    ${COLORS[index % COLORS.length]}
                                    ${isSelected ? 'scale-[0.98] opacity-100 ring-4 ring-white shadow-inner' : ''}
                                    ${isOtherSelected ? 'opacity-40 grayscale-[50%]' : 'hover:-translate-y-1 hover:shadow-lg'}
                                `}
                            >
                                <div className="w-12 flex justify-center shrink-0">
                                    {SHAPES[index % SHAPES.length]}
                                </div>
                                <span className="text-white text-lg md:text-2xl font-bold ml-4 text-left drop-shadow-sm leading-tight">
                                    {answer.text}
                                </span>

                                {isSelected && (
                                    <div className="absolute top-3 right-3 bg-black/30 rounded-full p-1.5">
                                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-6 h-6 text-white">
                                            <path fillRule="evenodd" d="M19.916 4.626a.75.75 0 01.208 1.04l-9 13.5a.75.75 0 01-1.154.114l-6-6a.75.75 0 011.06-1.06l5.353 5.353 8.493-12.739a.75.75 0 011.04-.208z" clipRule="evenodd" />
                                        </svg>
                                    </div>
                                )}
                            </button>
                        );
                    })}
                </div>

                {selectedAnswer !== null && (
                    <div className="text-center animate-pulse mb-4 md:mb-0">
                        <span className="bg-gray-800 text-white px-5 py-2 rounded-full font-bold text-sm shadow-md">
                            Відповідь прийнято. Очікуємо завершення часу...
                        </span>
                    </div>
                )}
            </main>
        </div>
    );
};

export default Game;
