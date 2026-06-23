import { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { api, currentUser } from '../net/api';

interface Answer {
    text: string;
    isCorrect: boolean;
}

interface Question {
    id: string;
    text: string;
    timeLimit: number;
    answers: Answer[];
}

const SHAPES = [
    <svg viewBox="0 0 24 24" fill="currentColor" className="w-6 h-6"><path d="M12 2L22 20H2Z" /></svg>,
    <svg viewBox="0 0 24 24" fill="currentColor" className="w-6 h-6"><path d="M12 2L22 12L12 22L2 12Z" /></svg>,
    <svg viewBox="0 0 24 24" fill="currentColor" className="w-6 h-6"><path d="M12 22C17.5228 22 22 17.5228 22 12C22 6.47715 17.5228 2 12 2C6.47715 2 2 6.47715 2 12C2 17.5228 6.47715 22 12 22Z" /></svg>,
    <svg viewBox="0 0 24 24" fill="currentColor" className="w-6 h-6"><path d="M3 3H21V21H3V3Z" /></svg>
];

const COLORS = [
    "bg-red-500", "bg-blue-500", "bg-yellow-500", "bg-green-500"
];

const TIME_LIMITS = [5, 10, 15, 20, 30, 60];

const QuizEditor = () => {
    const navigate = useNavigate();
    const location = useLocation();

    const editQuiz = location.state?.editQuiz;
    const editId: number | undefined = editQuiz?.id;

    const emptyQuestion = (): Question => ({
        id: Date.now().toString() + Math.floor(Math.random() * 1000),
        text: '',
        timeLimit: 15,
        answers: [
            { text: '', isCorrect: false },
            { text: '', isCorrect: false },
            { text: '', isCorrect: false },
            { text: '', isCorrect: false }
        ]
    });

    const [quizTitle, setQuizTitle] = useState(editQuiz?.title || '');
    const [description, setDescription] = useState(editQuiz?.description || '');
    const [questions, setQuestions] = useState<Question[]>([emptyQuestion()]);
    const [activeQuestionId, setActiveQuestionId] = useState<string>(questions[0].id);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        if (!editId) return;
        api.quiz(editId)
            .then((data) => {
                setQuizTitle(data.title);
                setDescription(data.description || '');
                const loaded: Question[] = (data.questions || []).map((q: any, i: number) => ({
                    id: `${i + 1}`,
                    text: q.text,
                    timeLimit: q.timeLimit,
                    answers: q.answers.map((a: any) => ({ text: a.text, isCorrect: a.isCorrect })),
                }));
                if (loaded.length > 0) {
                    setQuestions(loaded);
                    setActiveQuestionId(loaded[0].id);
                }
            })
            .catch((e) => setError(e.message));

    }, []);

    const activeQuestion = questions.find(q => q.id === activeQuestionId) || questions[0];

    const handleAddQuestion = () => {
        const newId = Date.now().toString();
        setQuestions([
            ...questions,
            {
                id: newId,
                text: '',
                timeLimit: 15,
                answers: [
                    { text: '', isCorrect: false },
                    { text: '', isCorrect: false },
                    { text: '', isCorrect: false },
                    { text: '', isCorrect: false }
                ]
            }
        ]);
        setActiveQuestionId(newId);
    };

    const handleDeleteQuestion = (id: string, e: React.MouseEvent) => {
        e.stopPropagation();
        if (questions.length === 1) return;

        const newQuestions = questions.filter(q => q.id !== id);
        setQuestions(newQuestions);
        if (activeQuestionId === id) {
            setActiveQuestionId(newQuestions[0].id);
        }
    };

    const updateActiveQuestion = (updates: Partial<Question>) => {
        setQuestions(questions.map(q =>
            q.id === activeQuestionId ? { ...q, ...updates } : q
        ));
    };

    const updateAnswer = (index: number, text: string) => {
        const newAnswers = [...activeQuestion.answers];
        newAnswers[index].text = text;
        updateActiveQuestion({ answers: newAnswers });
    };

    const toggleCorrectAnswer = (index: number) => {
        const newAnswers = [...activeQuestion.answers];
        newAnswers[index].isCorrect = !newAnswers[index].isCorrect;
        updateActiveQuestion({ answers: newAnswers });
    };

    const handleSave = async () => {
        setError('');
        if (!quizTitle.trim()) {
            setError('Введіть назву вікторини');
            return;
        }
        const payload = {
            title: quizTitle.trim(),
            description,
            creatorId: currentUser()?.id ?? 1,
            questions: questions.map((q) => ({
                text: q.text,
                timeLimit: q.timeLimit,
                answers: q.answers
                    .filter((a) => a.text.trim() !== '')
                    .map((a) => ({ text: a.text, isCorrect: a.isCorrect })),
            })),
        };
        setSaving(true);
        try {
            if (editId) {
                await api.updateQuiz(editId, payload);
            } else {
                await api.createQuiz(payload);
            }
            navigate('/dashboard');
        } catch (e: any) {
            setError(e.message || 'Не вдалося зберегти');
        } finally {
            setSaving(false);
        }
    };

    return (
        <div className="h-screen bg-gray-50 flex flex-col overflow-hidden">
            <header className="bg-white shadow-sm p-4 flex justify-between items-center z-10 border-b border-gray-200 shrink-0">
                <div className="flex items-center gap-4">
                    <button
                        onClick={() => navigate('/dashboard')}
                        className="p-2 hover:bg-gray-100 rounded-full transition-colors"
                    >
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor" className="w-6 h-6 text-gray-600">
                            <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 19.5L8.25 12l7.5-7.5" />
                        </svg>
                    </button>
                    <input
                        type="text"
                        placeholder="Введіть назву вікторини..."
                        value={quizTitle}
                        onChange={(e) => setQuizTitle(e.target.value)}
                        className="text-2xl font-bold text-gray-800 outline-none placeholder-gray-300 w-96 bg-transparent"
                    />
                </div>
                <div className="flex gap-3">
                    <button
                        onClick={() => navigate('/dashboard')}
                        className="px-6 py-2 rounded-md font-bold text-gray-600 hover:bg-gray-100 transition-colors"
                    >
                        Скасувати
                    </button>
                    <button
                        onClick={handleSave}
                        disabled={saving}
                        className="bg-blue-600 text-white px-8 py-2 rounded-md font-bold shadow-md hover:bg-blue-700 transition-colors disabled:bg-blue-400"
                    >
                        {saving ? 'Збереження...' : 'Зберегти'}
                    </button>
                </div>
            </header>

            {error && (
                <div className="bg-red-100 text-red-700 px-6 py-3 font-medium text-sm shrink-0">{error}</div>
            )}

            <div className="flex-1 flex overflow-hidden">
                <aside className="w-64 bg-white border-r border-gray-200 flex flex-col">
                    <div className="p-4 flex-1 overflow-y-auto custom-scrollbar">
                        {questions.map((q, index) => (
                            <div
                                key={q.id}
                                onClick={() => setActiveQuestionId(q.id)}
                                className={`relative p-3 mb-3 rounded-lg border-2 cursor-pointer transition-all ${activeQuestionId === q.id ? 'border-blue-500 bg-blue-50' : 'border-transparent bg-gray-100 hover:bg-gray-200'}`}
                            >
                                <div className="text-xs font-bold text-gray-500 mb-1">Запитання {index + 1}</div>
                                <div className="text-sm font-medium text-gray-800 truncate">
                                    {q.text || "Пусте запитання..."}
                                </div>
                                {questions.length > 1 && (
                                    <button
                                        onClick={(e) => handleDeleteQuestion(q.id, e)}
                                        className="absolute top-2 right-2 text-gray-400 hover:text-red-500"
                                    >
                                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-5 h-5">
                                            <path fillRule="evenodd" d="M8.75 1A2.75 2.75 0 006 3.75v.443c-.795.077-1.584.176-2.365.298a.75.75 0 10.23 1.482l.149-.022.841 10.518A2.75 2.75 0 007.596 19h4.807a2.75 2.75 0 002.742-2.53l.841-10.52.149.023a.75.75 0 00.23-1.482A41.03 41.03 0 0014 4.193V3.75A2.75 2.75 0 0011.25 1h-2.5zM10 4c.84 0 1.673.025 2.5.075V3.75c0-.69-.56-1.25-1.25-1.25h-2.5c-.69 0-1.25.56-1.25 1.25v.325C8.327 4.025 9.16 4 10 4zM8.58 7.72a.75.75 0 00-1.5.06l.3 7.5a.75.75 0 101.5-.06l-.3-7.5zm4.34.06a.75.75 0 10-1.5-.06l-.3 7.5a.75.75 0 101.5.06l.3-7.5z" clipRule="evenodd" />
                                        </svg>
                                    </button>
                                )}
                            </div>
                        ))}
                    </div>
                    <div className="p-4 border-t border-gray-200 bg-white shrink-0">
                        <button
                            onClick={handleAddQuestion}
                            className="w-full bg-blue-100 text-blue-700 py-3 rounded-lg font-bold hover:bg-blue-200 transition-colors flex justify-center items-center gap-2"
                        >
                            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2.5} stroke="currentColor" className="w-5 h-5">
                                <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
                            </svg>
                            Додати запитання
                        </button>
                    </div>
                </aside>

                <main className="flex-1 p-8 overflow-y-auto bg-gray-100 flex flex-col gap-6">
                    <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-200 flex flex-col items-center">
                        <div className="w-full flex justify-end mb-4">
                            <div className="flex items-center gap-3 bg-gray-50 px-4 py-2 rounded-lg border border-gray-200">
                                <span className="text-sm font-bold text-gray-500">Ліміт часу:</span>
                                <select
                                    value={activeQuestion.timeLimit}
                                    onChange={(e) => updateActiveQuestion({ timeLimit: Number(e.target.value) })}
                                    className="bg-transparent font-bold text-gray-800 outline-none cursor-pointer"
                                >
                                    {TIME_LIMITS.map(time => (
                                        <option key={time} value={time}>{time} секунд</option>
                                    ))}
                                </select>
                            </div>
                        </div>

                        <input
                            type="text"
                            placeholder="Почніть вводити запитання..."
                            value={activeQuestion.text}
                            onChange={(e) => updateActiveQuestion({ text: e.target.value })}
                            className="w-full text-center text-3xl font-bold text-gray-800 outline-none placeholder-gray-300 py-8"
                        />
                    </div>

                    <div className="grid grid-cols-2 gap-4 flex-1">
                        {activeQuestion.answers.map((answer, index) => (
                            <div key={index} className={`relative flex items-center p-2 rounded-xl shadow-sm ${COLORS[index]}`}>
                                <div className="w-14 flex justify-center text-white shrink-0">
                                    {SHAPES[index]}
                                </div>
                                <input
                                    type="text"
                                    placeholder={`Додайте варіант ${index + 1}`}
                                    value={answer.text}
                                    onChange={(e) => updateAnswer(index, e.target.value)}
                                    className="flex-1 bg-white/90 text-gray-800 font-bold text-xl py-4 px-4 rounded-lg outline-none placeholder-gray-400 focus:bg-white transition-colors"
                                />
                                <button
                                    onClick={() => toggleCorrectAnswer(index)}
                                    className={`absolute right-6 w-10 h-10 rounded-full flex items-center justify-center transition-all border-4 ${
                                        answer.isCorrect
                                            ? 'bg-green-500 border-white text-white'
                                            : 'bg-white border-transparent text-gray-300 hover:text-green-500'
                                    }`}
                                    title="Позначити як правильну відповідь"
                                >
                                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-6 h-6">
                                        <path fillRule="evenodd" d="M19.916 4.626a.75.75 0 01.208 1.04l-9 13.5a.75.75 0 01-1.154.114l-6-6a.75.75 0 011.06-1.06l5.353 5.353 8.493-12.739a.75.75 0 011.04-.208z" clipRule="evenodd" />
                                    </svg>
                                </button>
                            </div>
                        ))}
                    </div>
                </main>
            </div>
        </div>
    );
};

export default QuizEditor;
