import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

const MOCK_QUESTIONS = [
    {
        id: 1,
        text: "Який протокол використовується для безпечного передавання гіпертексту?",
        answers: ["HTTP", "FTP", "HTTPS", "TCP/IP"],
        timeLimit: 5
    },
    {
        id: 2,
        text: "Що з переліченого НЕ є мовою програмування?",
        answers: ["Java", "HTML", "Python", "C++"],
        timeLimit: 5
    }
];

const MOCK_LEADERBOARD = [
    { rank: 1, username: "nazar", score: 4500 },
    { rank: 2, username: "nikita", score: 4250 },
    { rank: 3, username: "student_123", score: 3800 },
    { rank: 4, username: "hacker_boy", score: 3100 },
];

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
    const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
    const [timeLeft, setTimeLeft] = useState(MOCK_QUESTIONS[0].timeLimit);
    const [selectedAnswer, setSelectedAnswer] = useState<number | null>(null);
    const [view, setView] = useState<'QUESTION' | 'LEADERBOARD' | 'PODIUM'>('QUESTION');
    const [errorMsg, setErrorMsg] = useState('');
    const [isAuthenticated] = useState(false);
    const [isKicked] = useState(false);

    const activeQuestion = MOCK_QUESTIONS[currentQuestionIndex];

    useEffect(() => {
        if (isKicked) return;

        if (view === 'QUESTION') {
            if (timeLeft > 0) {
                const timerId = setTimeout(() => setTimeLeft(timeLeft - 1), 1000);
                return () => clearTimeout(timerId);
            } else {
                setView('LEADERBOARD');
            }
        } else if (view === 'LEADERBOARD') {
            const timerId = setTimeout(() => {
                if (currentQuestionIndex < MOCK_QUESTIONS.length - 1) {
                    const nextIndex = currentQuestionIndex + 1;
                    setCurrentQuestionIndex(nextIndex);
                    setSelectedAnswer(null);
                    setTimeLeft(MOCK_QUESTIONS[nextIndex].timeLimit);
                    setView('QUESTION');
                } else {
                    setView('PODIUM');
                }
            }, 4000);
            return () => clearTimeout(timerId);
        }
    }, [timeLeft, view, currentQuestionIndex, isKicked]);

    const handleAnswerClick = (index: number) => {
        if (selectedAnswer !== null) {
            setErrorMsg('Ви вже надіслали відповідь на це запитання!');
            setTimeout(() => setErrorMsg(''), 3000);
            return;
        }
        setSelectedAnswer(index);
    };

    if (isKicked) {
        return (
            <div className="min-h-screen bg-gray-900 flex flex-col items-center justify-center p-4">
                <div className="bg-white p-8 rounded-3xl shadow-2xl text-center max-w-md w-full">
                    <div className="w-24 h-24 bg-red-50 rounded-full flex items-center justify-center mx-auto mb-6">
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor" className="w-12 h-12 text-red-500">
                            <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                        </svg>
                    </div>
                    <h2 className="text-3xl font-black text-gray-800 mb-4">Гру завершено</h2>
                    <p className="text-gray-600 mb-8 font-medium text-lg leading-snug">
                        Адміністратор примусово завершив цю ігрову сесію.
                    </p>
                    <div className="flex flex-col gap-3">
                        <button
                            onClick={() => navigate('/')}
                            className="w-full bg-gray-100 text-gray-800 px-6 py-4 rounded-xl font-bold hover:bg-gray-200 transition-colors"
                        >
                            Повернутися на головну
                        </button>
                        {isAuthenticated && (
                            <button
                                onClick={() => navigate('/dashboard')}
                                className="w-full bg-blue-600 text-white px-6 py-4 rounded-xl font-bold hover:bg-blue-700 transition-colors shadow-md"
                            >
                                Перейти в Дашборд
                            </button>
                        )}
                    </div>
                </div>
            </div>
        );
    }

    if (view === 'PODIUM') {
        return (
            <div className="min-h-screen bg-blue-600 flex flex-col items-center justify-center p-4">
                <h1 className="text-white text-5xl font-black mb-19 drop-shadow-lg text-center tracking-wide">
                    Переможці:
                </h1>

                <div className="flex items-end justify-center gap-2 md:gap-4 h-64 md:h-80 mb-12 w-full max-w-3xl">
                    <div className="w-1/3 bg-gray-300 h-[70%] rounded-t-lg shadow-2xl flex flex-col items-center pt-4 relative">
                        <div className="absolute -top-6 bg-white rounded-full px-3 py-1 font-bold text-gray-500 shadow-sm text-sm">@{MOCK_LEADERBOARD[1].username}</div>
                        <span className="text-5xl font-black text-gray-100 drop-shadow-md">2</span>
                        <span className="mt-auto mb-4 font-bold text-gray-600">{MOCK_LEADERBOARD[1].score}</span>
                    </div>
                    <div className="w-1/3 bg-yellow-400 h-full rounded-t-lg shadow-2xl flex flex-col items-center pt-4 relative z-10 border-t-4 border-yellow-300">
                        <div className="absolute -top-8 bg-white rounded-full px-4 py-1.5 font-black text-yellow-500 shadow-md">@{MOCK_LEADERBOARD[0].username}</div>
                        <span className="text-7xl font-black text-yellow-200 drop-shadow-md">1</span>
                        <span className="mt-auto mb-4 font-bold text-yellow-700 text-lg">{MOCK_LEADERBOARD[0].score}</span>
                    </div>
                    <div className="w-1/3 bg-orange-400 h-[50%] rounded-t-lg shadow-2xl flex flex-col items-center pt-4 relative">
                        <div className="absolute -top-6 bg-white rounded-full px-3 py-1 font-bold text-orange-500 shadow-sm text-sm">@{MOCK_LEADERBOARD[2].username}</div>
                        <span className="text-4xl font-black text-orange-200 drop-shadow-md">3</span>
                        <span className="mt-auto mb-4 font-bold text-orange-800">{MOCK_LEADERBOARD[2].score}</span>
                    </div>
                </div>

                <div className="flex justify-center gap-4 w-full max-w-md bg-white p-6 rounded-2xl shadow-xl">
                    <button
                        onClick={() => navigate('/')}
                        className="flex-1 bg-gray-100 text-gray-700 px-6 py-3 rounded-lg font-bold hover:bg-gray-200 transition-colors"
                    >
                        На головну
                    </button>
                    {isAuthenticated && (
                        <button
                            onClick={() => navigate('/dashboard')}
                            className="flex-1 bg-blue-600 text-white px-6 py-3 rounded-lg font-bold hover:bg-blue-700 transition-colors shadow-md"
                        >
                            В Дашборд
                        </button>
                    )}
                </div>
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
                        {MOCK_LEADERBOARD.map((player, index) => (
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
                                    <span className="w-8 text-center">{player.rank}.</span>
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
        <div className="min-h-screen bg-gray-100 flex flex-col relative">
            <header className="bg-white shadow-sm p-4 flex justify-between items-center z-10">
                <div className="text-xl md:text-2xl font-black text-blue-600 tracking-tighter">KMAhoot!</div>
                <div className="text-gray-500 font-bold text-sm md:text-base">Питання {currentQuestionIndex + 1} з {MOCK_QUESTIONS.length}</div>
                <div className="bg-gray-900 text-white px-4 py-1.5 rounded-full font-bold text-sm md:text-base">
                    PIN: 482910
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
                        {activeQuestion.text}
                    </h1>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 h-auto md:h-48">
                    {activeQuestion.answers.map((answer, index) => {
                        const isSelected = selectedAnswer === index;
                        const isOtherSelected = selectedAnswer !== null && selectedAnswer !== index;

                        return (
                            <button
                                key={index}
                                onClick={() => handleAnswerClick(index)}
                                disabled={selectedAnswer !== null}
                                className={`
                                    relative flex items-center p-4 rounded-xl border-b-[6px] transition-all duration-200
                                    ${COLORS[index]}
                                    ${isSelected ? 'scale-[0.98] opacity-100 ring-4 ring-white shadow-inner' : ''}
                                    ${isOtherSelected ? 'opacity-40 grayscale-[50%]' : 'hover:-translate-y-1 hover:shadow-lg'}
                                `}
                            >
                                <div className="w-12 flex justify-center shrink-0">
                                    {SHAPES[index]}
                                </div>
                                <span className="text-white text-lg md:text-2xl font-bold ml-4 text-left drop-shadow-sm leading-tight">
                                    {answer}
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