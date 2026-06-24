import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

interface Quiz {
    id: number;
    title: string;
    questionsCount: number;
    date: string;
    author?: string;
}

const MOCK_QUIZZES: Quiz[] = [
    { id: 1, title: 'Основи баз даних', questionsCount: 15, date: '2026-06-20' },
    { id: 2, title: 'Мережеві протоколи', questionsCount: 20, date: '2026-06-22' },
    { id: 3, title: 'React + TypeScript', questionsCount: 12, date: '2026-06-24' }
];

const MOCK_GLOBAL_QUIZZES: Quiz[] = [
    ...MOCK_QUIZZES,
    { id: 4, title: 'Історія України', questionsCount: 40, date: '2026-06-18', author: 'historian_pro' },
    { id: 5, title: 'Лінійна алгебра', questionsCount: 10, date: '2026-06-19', author: 'math_teacher' }
];

const MOCK_HISTORY = [
    { id: 1, title: 'Основи баз даних', date: '2026-06-21', players: 24, winner: 'student_123' },
    { id: 2, title: 'Мережеві протоколи', date: '2026-06-23', players: 18, winner: 'hacker_boy' }
];

const MOCK_STATS = {
    hostedSessions: 15,
    totalPlayersHosted: 342,
    gamesPlayedAsPlayer: 45,
    averageRank: '#3',
    winRate: '68%'
};

const MOCK_ACTIVE_SESSIONS = [
    { pin: '482910', host: 'nazar', quizTitle: 'Основи баз даних', players: 12 },
    { pin: '749201', host: 'nikita', quizTitle: 'React для початківців', players: 35 }
];

const MOCK_USERS = [
    { id: 1, username: 'nazar', role: 'ADMIN', status: 'ACTIVE' },
    { id: 2, username: 'nikita', role: 'PLAYER', status: 'ACTIVE' },
    { id: 3, username: 'spammer_99', role: 'PLAYER', status: 'BLOCKED' },
    { id: 4, username: 'student_123', role: 'PLAYER', status: 'ACTIVE' }
];

const Dashboard = () => {
    const [role, setRole] = useState<'PLAYER' | 'ADMIN'>('PLAYER');
    const [activeTab, setActiveTab] = useState('my-quizzes');
    const [joinPin, setJoinPin] = useState('');
    const navigate = useNavigate();

    const handleJoinGame = (e: React.FormEvent) => {
        e.preventDefault();
        if (joinPin.trim()) {
            navigate(`/lobby/${joinPin}`, { state: { username: 'user123' } });
        }
    };

    const navGap = role === 'ADMIN' ? 'gap-1.5' : 'gap-4';
    const btnPadding = role === 'ADMIN' ? 'py-2.5' : 'py-4';

    return (
        <div className="min-h-screen bg-gray-50 flex">
            <aside className="w-64 bg-white shadow-md flex flex-col z-10">
                <div className="p-6 border-b">
                    <h2 className="text-2xl tracking-tighter font-black text-blue-600">KMAhoot!</h2>
                    <p className="text-sm text-gray-500 mt-1 font-medium">
                        {role === 'ADMIN' ? 'Адміністратор' : 'Ведучий / Гравець'}
                    </p>
                </div>

                <nav className={`flex-1 p-3 flex flex-col overflow-y-auto ${navGap}`}>
                    <button
                        onClick={() => setActiveTab('my-quizzes')}
                        className={`text-left px-4 ${btnPadding} rounded-md font-medium transition-all ${activeTab === 'my-quizzes' ? 'bg-blue-50 text-blue-700 shadow-sm' : 'text-gray-600 hover:bg-gray-50'}`}
                    >
                        Мої вікторини
                    </button>

                    <button
                        onClick={() => setActiveTab('history')}
                        className={`text-left px-4 ${btnPadding} rounded-md font-medium transition-all ${activeTab === 'history' ? 'bg-blue-50 text-blue-700 shadow-sm' : 'text-gray-600 hover:bg-gray-50'}`}
                    >
                        Історія ігор
                    </button>

                    <button
                        onClick={() => setActiveTab('statistics')}
                        className={`text-left px-4 ${btnPadding} rounded-md font-medium transition-all ${activeTab === 'statistics' ? 'bg-blue-50 text-blue-700 shadow-sm' : 'text-gray-600 hover:bg-gray-50'}`}
                    >
                        Статистика
                    </button>

                    {role === 'ADMIN' && (
                        <>
                            <div className="my-2 border-t border-gray-100"></div>
                            <div className="px-4 py-1 text-xs font-bold text-gray-400 uppercase tracking-wider">
                                Адміністрування
                            </div>

                            <button
                                onClick={() => setActiveTab('global-quizzes')}
                                className={`text-left px-4 ${btnPadding} rounded-md font-medium transition-all ${activeTab === 'global-quizzes' ? 'bg-blue-50 text-blue-700 shadow-sm' : 'text-gray-600 hover:bg-gray-50'}`}
                            >
                                Глобальний каталог
                            </button>

                            <button
                                onClick={() => setActiveTab('admin-panel')}
                                className={`text-left px-4 ${btnPadding} rounded-md font-medium transition-all ${activeTab === 'admin-panel' ? 'bg-blue-50 text-blue-700 shadow-sm' : 'text-gray-600 hover:bg-gray-50'}`}
                            >
                                Активні сесії
                            </button>

                            <button
                                onClick={() => setActiveTab('users')}
                                className={`text-left px-4 ${btnPadding} rounded-md font-medium transition-all ${activeTab === 'users' ? 'bg-blue-50 text-blue-700 shadow-sm' : 'text-gray-600 hover:bg-gray-50'}`}
                            >
                                Користувачі
                            </button>
                        </>
                    )}
                </nav>

                <div className="p-2 border-t">
                    <button
                        onClick={() => navigate('/')}
                        className="w-full flex items-center justify-start gap-2 px-3 py-3 text-gray-600 hover:bg-gray-100 hover:text-gray-900 rounded-md font-bold transition-colors"
                    >
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor" className="w-5 h-5">
                            <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15M12 9l-3 3m0 0l3 3m-3-3h12.75" />
                        </svg>
                        Вийти
                    </button>
                </div>
            </aside>

            <main className="flex-1 p-8 overflow-y-auto">
                <div className="max-w-6xl mx-auto">
                    <div className="flex justify-between items-center mb-8 border-b border-gray-200 pb-5">
                        <div>
                            {activeTab === 'my-quizzes' && <h1 className="text-3xl font-bold text-gray-800">Мої вікторини</h1>}
                            {activeTab === 'history' && <h1 className="text-3xl font-bold text-gray-800">Історія ігор</h1>}
                            {activeTab === 'statistics' && <h1 className="text-3xl font-bold text-gray-800">Особиста статистика</h1>}
                            {activeTab === 'global-quizzes' && <h1 className="text-3xl font-bold text-gray-800">Глобальний каталог квізів</h1>}
                            {activeTab === 'admin-panel' && <h1 className="text-3xl font-bold text-gray-800">Активні ігрові сесії</h1>}
                            {activeTab === 'users' && <h1 className="text-3xl font-bold text-gray-800">Управління користувачами</h1>}
                        </div>

                        <div className="flex items-center gap-4">
                            <form onSubmit={handleJoinGame} className="flex items-center gap-2 bg-white p-1.5 border border-gray-300 rounded-lg shadow-sm">
                                <input
                                    type="text"
                                    placeholder="Введіть PIN-код гри"
                                    value={joinPin}
                                    onChange={(e) => setJoinPin(e.target.value)}
                                    maxLength={6}
                                    className="px-3 py-2 text-sm outline-none text-center font-bold w-40 bg-transparent"
                                />
                                <button
                                    type="submit"
                                    className="bg-gray-900 hover:bg-gray-800 text-white px-4 py-2 rounded-md text-sm font-bold transition-colors flex items-center gap-1"
                                >
                                    Увійти
                                </button>
                            </form>

                            {activeTab === 'my-quizzes' && (
                                <button onClick={() => navigate('/create')} className="bg-blue-600 text-white px-6 py-3 rounded-md font-bold hover:bg-blue-700 transition-colors shadow-md flex items-center gap-2">
                                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2.5} stroke="currentColor" className="w-5 h-5">
                                        <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
                                    </svg>
                                    Створити нову
                                </button>
                            )}
                        </div>
                    </div>

                    {activeTab === 'my-quizzes' && (
                        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
                            {MOCK_QUIZZES.map((quiz) => (
                                <div key={quiz.id} className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden hover:shadow-md transition-shadow flex flex-col">
                                    <div className="h-32 bg-gradient-to-r from-blue-400 to-indigo-500"></div>
                                    <div className="p-5 flex-1 flex flex-col">
                                        <h3 className="text-xl font-bold text-gray-800 mb-2">{quiz.title}</h3>
                                        <div className="text-sm text-gray-500 font-medium mb-4 flex-1">
                                            {quiz.questionsCount} запитань • Створено {quiz.date}
                                        </div>
                                        <div className="flex gap-3 mt-auto">
                                            <button onClick={() => navigate('/host/482910')} className="flex-1 bg-green-500 text-white py-2 rounded-md font-bold hover:bg-green-600 transition-colors">
                                                Запустити
                                            </button>
                                            <button onClick={() => navigate('/create', { state: { editQuiz: quiz } })} className="px-4 bg-gray-100 text-gray-700 py-2 rounded-md font-bold hover:bg-gray-200 transition-colors">
                                                Редагувати
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}

                    {activeTab === 'history' && (
                        <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
                            <table className="w-full text-left border-collapse">
                                <thead>
                                <tr className="bg-gray-50 border-b border-gray-200">
                                    <th className="p-4 text-sm font-bold text-gray-600">Назва вікторини</th>
                                    <th className="p-4 text-sm font-bold text-gray-600">Дата</th>
                                    <th className="p-4 text-sm font-bold text-gray-600">Гравців</th>
                                    <th className="p-4 text-sm font-bold text-gray-600">Переможець</th>
                                </tr>
                                </thead>
                                <tbody>
                                {MOCK_HISTORY.map((session) => (
                                    <tr key={session.id} className="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                                        <td className="p-4 font-bold text-gray-800">{session.title}</td>
                                        <td className="p-4 text-gray-600 font-medium">{session.date}</td>
                                        <td className="p-4 text-gray-600 font-medium">{session.players}</td>
                                        <td className="p-4 text-green-600 font-bold">{session.winner}</td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    )}

                    {activeTab === 'statistics' && (
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                            <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
                                <div className="text-gray-500 text-sm font-bold uppercase tracking-wider mb-2">Проведено ігор</div>
                                <div className="text-4xl font-black text-blue-600">{MOCK_STATS.hostedSessions}</div>
                            </div>
                            <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
                                <div className="text-gray-500 text-sm font-bold uppercase tracking-wider mb-2">Всього учасників</div>
                                <div className="text-4xl font-black text-blue-600">{MOCK_STATS.totalPlayersHosted}</div>
                            </div>
                            <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
                                <div className="text-gray-500 text-sm font-bold uppercase tracking-wider mb-2">Зіграно як гравець</div>
                                <div className="text-4xl font-black text-green-500">{MOCK_STATS.gamesPlayedAsPlayer}</div>
                            </div>
                            <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
                                <div className="text-gray-500 text-sm font-bold uppercase tracking-wider mb-2">Відсоток перемог</div>
                                <div className="text-4xl font-black text-indigo-500">{MOCK_STATS.winRate}</div>
                            </div>
                            <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
                                <div className="text-gray-500 text-sm font-bold uppercase tracking-wider mb-2">Середнє місце</div>
                                <div className="text-4xl font-black text-purple-500">{MOCK_STATS.averageRank}</div>
                            </div>
                        </div>
                    )}

                    {activeTab === 'global-quizzes' && role === 'ADMIN' && (
                        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
                            {MOCK_GLOBAL_QUIZZES.map((quiz) => (
                                <div key={quiz.id} className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden flex flex-col">
                                    <div className="p-5 flex-1 flex flex-col">
                                        <h3 className="text-xl font-bold text-gray-800 mb-1">{quiz.title}</h3>
                                        <div className="text-sm text-gray-500 font-medium mb-1">
                                            Автор: <span className="text-blue-600">@{quiz.author || 'system'}</span>
                                        </div>
                                        <div className="text-sm text-gray-500 font-medium mb-4 flex-1">
                                            {quiz.questionsCount} запитань • Створено {quiz.date}
                                        </div>
                                        <div className="flex gap-2 mt-auto">
                                            <button className="flex-1 bg-gray-100 text-gray-700 py-2 rounded-md font-bold hover:bg-gray-200 transition-colors">
                                                Модерувати
                                            </button>
                                            <button className="px-4 bg-red-100 text-red-700 py-2 rounded-md font-bold hover:bg-red-600 hover:text-white transition-colors">
                                                Видалити
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}

                    {activeTab === 'admin-panel' && role === 'ADMIN' && (
                        <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
                            <table className="w-full text-left border-collapse">
                                <thead>
                                <tr className="bg-gray-50 border-b border-gray-200">
                                    <th className="p-4 text-sm font-bold text-gray-600">PIN-код</th>
                                    <th className="p-4 text-sm font-bold text-gray-600">Ведучий</th>
                                    <th className="p-4 text-sm font-bold text-gray-600">Вікторина</th>
                                    <th className="p-4 text-sm font-bold text-gray-600">Гравців</th>
                                    <th className="p-4 text-sm font-bold text-gray-600 text-right">Дія</th>
                                </tr>
                                </thead>
                                <tbody>
                                {MOCK_ACTIVE_SESSIONS.map((session) => (
                                    <tr key={session.pin} className="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                                        <td className="p-4 font-bold text-gray-900 tracking-wider">{session.pin}</td>
                                        <td className="p-4 text-gray-700 font-medium">@{session.host}</td>
                                        <td className="p-4 text-gray-700 font-medium">{session.quizTitle}</td>
                                        <td className="p-4 text-gray-700 font-medium">{session.players}</td>
                                        <td className="p-4 text-right">
                                            <button className="bg-red-100 text-red-700 px-4 py-2 rounded-md font-bold text-sm hover:bg-red-600 hover:text-white transition-colors">
                                                Завершити примусово
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    )}

                    {activeTab === 'users' && role === 'ADMIN' && (
                        <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
                            <table className="w-full text-left border-collapse">
                                <thead>
                                <tr className="bg-gray-50 border-b border-gray-200">
                                    <th className="p-4 text-sm font-bold text-gray-600">ID</th>
                                    <th className="p-4 text-sm font-bold text-gray-600">Користувач</th>
                                    <th className="p-4 text-sm font-bold text-gray-600">Роль</th>
                                    <th className="p-4 text-sm font-bold text-gray-600">Статус</th>
                                    <th className="p-4 text-sm font-bold text-gray-600 text-right">Дія</th>
                                </tr>
                                </thead>
                                <tbody>
                                {MOCK_USERS.map((user) => (
                                    <tr key={user.id} className="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                                        <td className="p-4 text-gray-500 font-medium">{user.id}</td>
                                        <td className="p-4 font-bold text-gray-800">@{user.username}</td>
                                        <td className="p-4 text-gray-700 font-medium">{user.role}</td>
                                        <td className="p-4 font-bold">
                                            {user.status === 'ACTIVE' ? (
                                                <span className="text-green-500">Активний</span>
                                            ) : (
                                                <span className="text-red-500">Заблокований</span>
                                            )}
                                        </td>
                                        <td className="p-4 text-right">
                                            {user.status === 'ACTIVE' ? (
                                                <button className="bg-red-100 text-red-700 px-4 py-2 rounded-md font-bold text-sm hover:bg-red-600 hover:text-white transition-colors">
                                                    Блокувати
                                                </button>
                                            ) : (
                                                <button className="bg-green-100 text-green-700 px-4 py-2 rounded-md font-bold text-sm hover:bg-green-600 hover:text-white transition-colors">
                                                    Розблокувати
                                                </button>
                                            )}
                                        </td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>
            </main>

            <button
                onClick={() => {
                    const newRole = role === 'PLAYER' ? 'ADMIN' : 'PLAYER';
                    setRole(newRole);
                    if (newRole === 'PLAYER' && ['admin-panel', 'global-quizzes', 'users'].includes(activeTab)) {
                        setActiveTab('my-quizzes');
                    }
                }}
                className="fixed bottom-6 right-6 bg-gray-900 text-white px-6 py-3 rounded-full shadow-xl text-sm font-bold hover:bg-gray-800 transition-transform hover:scale-105"
            >
                Переключити на {role === 'PLAYER' ? 'ADMIN' : 'PLAYER'}
            </button>
        </div>
    );
};

export default Dashboard;