import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { gameClient, MessageType } from '../net/gameClient';
import { api, currentUser, clearUser } from '../net/api';

interface ApiQuiz {
    id: number;
    title: string;
    description: string;
    questionCount: number;
}

interface AdminUser {
    id: number;
    username: string;
    role: string;
    status: string;
}

interface ApiSession {
    pin: string;
    quizTitle: string;
    players: number;
    state: string;
}

interface HistoryEntry {
    quizTitle: string;
    playedAt: string;
    winner: string;
    players: number;
}

const Dashboard = () => {
    const navigate = useNavigate();
    const user = currentUser();
    const role = user?.role === 'ADMIN' ? 'ADMIN' : 'PLAYER';

    const [activeTab, setActiveTab] = useState('my-quizzes');
    const [joinPin, setJoinPin] = useState('');
    const [quizzes, setQuizzes] = useState<ApiQuiz[]>([]);
    const [users, setUsers] = useState<AdminUser[]>([]);
    const [sessions, setSessions] = useState<ApiSession[]>([]);
    const [history, setHistory] = useState<HistoryEntry[]>([]);
    const [error, setError] = useState('');

    const fetchQuizzes = () => api.quizzes().then(setQuizzes).catch((e) => setError(e.message));
    const fetchUsers = () => api.users().then(setUsers).catch((e) => setError(e.message));
    const fetchSessions = () => api.sessions().then(setSessions).catch((e) => setError(e.message));
    const fetchHistory = () => api.history().then(setHistory).catch((e) => setError(e.message));

    useEffect(() => {
        if (!user) {
            navigate('/authorization');
            return;
        }
        fetchQuizzes();
        fetchHistory();
        if (role === 'ADMIN') {
            fetchUsers();
            fetchSessions();
        }

    }, []);

    const totalGames = history.length;
    const totalPlayers = history.reduce((sum, h) => sum + h.players, 0);

    useEffect(() => {
        if (role !== 'ADMIN' || activeTab !== 'admin-panel') return;
        const id = setInterval(fetchSessions, 3000);
        return () => clearInterval(id);

    }, [activeTab, role]);

    useEffect(() => {
        if (!user) return;
        const check = () => api.me(user.id)
            .then((u) => {
                if (u.status === 'BLOCKED') {
                    clearUser();
                    alert('Ваш акаунт заблоковано адміністратором');
                    navigate('/');
                }
            })
            .catch(() => {});
        check();
        const id = setInterval(check, 8000);
        return () => clearInterval(id);

    }, []);

    if (!user) {
        return null;
    }

    const handleJoinGame = (e: React.FormEvent) => {
        e.preventDefault();
        if (joinPin.trim()) {
            navigate(`/lobby/${joinPin}`, { state: { username: user.username } });
        }
    };

    const handleHost = async (quizId: number) => {
        try {
            await gameClient.connect();
            const created = gameClient.once(MessageType.ROOM_CREATED);
            gameClient.send(MessageType.REQ_CREATE_ROOM, { quizId, userId: user.id });
            const pkt = await created;
            gameClient.setSession(pkt.payload.pin, null, 'HOST');
            navigate(`/host/${pkt.payload.pin}`);
        } catch (err: any) {
            alert(err.message || 'Не вдалося створити кімнату');
        }
    };

    const handleDeleteQuiz = async (id: number) => {
        try {
            await api.deleteQuiz(id);
            fetchQuizzes();
        } catch (err: any) {
            setError(err.message);
        }
    };

    const handleToggleUser = async (u: AdminUser) => {
        try {
            await api.setUserStatus(u.id, u.status === 'ACTIVE' ? 'BLOCKED' : 'ACTIVE');
            fetchUsers();
        } catch (err: any) {
            setError(err.message);
        }
    };

    const handleEndSession = async (pin: string) => {
        try {
            await api.endSession(pin);
            fetchSessions();
        } catch (err: any) {
            setError(err.message);
        }
    };

    const logout = () => {
        clearUser();
        navigate('/');
    };

    return (
        <div className="min-h-screen bg-gray-50 flex">
            <aside className="w-64 bg-white shadow-md flex flex-col z-10">
                <div className="p-6 border-b">
                    <h2 className="text-2xl tracking-tighter font-black text-blue-600">KMAhoot!</h2>
                    <p className="text-sm text-gray-500 mt-1 font-medium">
                        @{user.username} • {role === 'ADMIN' ? 'Адміністратор' : 'Ведучий / Гравець'}
                    </p>
                </div>

                <nav className="flex-1 p-3 flex flex-col overflow-y-auto gap-2">
                    <button onClick={() => setActiveTab('my-quizzes')} className={tabClass(activeTab === 'my-quizzes')}>Мої вікторини</button>
                    <button onClick={() => setActiveTab('history')} className={tabClass(activeTab === 'history')}>Історія ігор</button>
                    <button onClick={() => setActiveTab('statistics')} className={tabClass(activeTab === 'statistics')}>Статистика</button>

                    {role === 'ADMIN' && (
                        <>
                            <div className="my-2 border-t border-gray-100"></div>
                            <div className="px-4 py-1 text-xs font-bold text-gray-400 uppercase tracking-wider">Адміністрування</div>
                            <button onClick={() => setActiveTab('global-quizzes')} className={tabClass(activeTab === 'global-quizzes')}>Глобальний каталог</button>
                            <button onClick={() => setActiveTab('admin-panel')} className={tabClass(activeTab === 'admin-panel')}>Активні сесії</button>
                            <button onClick={() => setActiveTab('users')} className={tabClass(activeTab === 'users')}>Користувачі</button>
                        </>
                    )}
                </nav>

                <div className="p-2 border-t">
                    <button onClick={logout} className="w-full flex items-center justify-start gap-2 px-3 py-3 text-gray-600 hover:bg-gray-100 hover:text-gray-900 rounded-md font-bold transition-colors">
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
                        <h1 className="text-3xl font-bold text-gray-800">{titleFor(activeTab)}</h1>
                        <div className="flex items-center gap-4">
                            <form onSubmit={handleJoinGame} className="flex items-center gap-2 bg-white p-1.5 border border-gray-300 rounded-lg shadow-sm">
                                <input type="text" placeholder="PIN-код гри" value={joinPin} onChange={(e) => setJoinPin(e.target.value)} maxLength={6}
                                       className="px-3 py-2 text-sm outline-none text-center font-bold w-36 bg-transparent" />
                                <button type="submit" className="bg-gray-900 hover:bg-gray-800 text-white px-4 py-2 rounded-md text-sm font-bold transition-colors">Увійти</button>
                            </form>
                            {activeTab === 'my-quizzes' && (
                                <button onClick={() => navigate('/create')} className="bg-blue-600 text-white px-6 py-3 rounded-md font-bold hover:bg-blue-700 transition-colors shadow-md">
                                    + Створити нову
                                </button>
                            )}
                        </div>
                    </div>

                    {error && <div className="mb-6 p-4 bg-red-100 text-red-700 rounded-lg font-medium">{error} — чи запущено сервер?</div>}

                    {(activeTab === 'my-quizzes' || activeTab === 'global-quizzes') && (
                        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
                            {quizzes.length === 0 && <div className="text-gray-400 font-medium">Вікторин поки немає.</div>}
                            {quizzes.map((quiz) => (
                                <div key={quiz.id} className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden hover:shadow-md transition-shadow flex flex-col">
                                    <div className="h-24 bg-gradient-to-r from-blue-400 to-indigo-500"></div>
                                    <div className="p-5 flex-1 flex flex-col">
                                        <h3 className="text-xl font-bold text-gray-800 mb-1">{quiz.title}</h3>
                                        <div className="text-sm text-gray-500 font-medium mb-2">{quiz.questionCount} запитань</div>
                                        <div className="text-sm text-gray-400 mb-4 flex-1">{quiz.description}</div>
                                        <div className="flex gap-3 mt-auto">
                                            {activeTab === 'my-quizzes' ? (
                                                <>
                                                    <button onClick={() => handleHost(quiz.id)} className="flex-1 bg-green-500 text-white py-2 rounded-md font-bold hover:bg-green-600 transition-colors">Запустити</button>
                                                    <button onClick={() => navigate('/create', { state: { editQuiz: quiz } })} className="px-4 bg-gray-100 text-gray-700 py-2 rounded-md font-bold hover:bg-gray-200 transition-colors">Редагувати</button>
                                                </>
                                            ) : (
                                                <button onClick={() => handleDeleteQuiz(quiz.id)} className="flex-1 bg-red-100 text-red-700 py-2 rounded-md font-bold hover:bg-red-600 hover:text-white transition-colors">Видалити</button>
                                            )}
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}

                    {activeTab === 'history' && (
                        <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
                            <table className="w-full text-left border-collapse">
                                <thead><tr className="bg-gray-50 border-b border-gray-200">
                                    <th className="p-4 text-sm font-bold text-gray-600">Назва вікторини</th>
                                    <th className="p-4 text-sm font-bold text-gray-600">Дата</th>
                                    <th className="p-4 text-sm font-bold text-gray-600">Гравців</th>
                                    <th className="p-4 text-sm font-bold text-gray-600">Переможець</th>
                                </tr></thead>
                                <tbody>
                                {history.length === 0 && <tr><td colSpan={4} className="p-6 text-center text-gray-400 font-medium">Ще не зіграно жодної гри</td></tr>}
                                {history.map((s, i) => (
                                    <tr key={i} className="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                                        <td className="p-4 font-bold text-gray-800">{s.quizTitle}</td>
                                        <td className="p-4 text-gray-600 font-medium">{s.playedAt}</td>
                                        <td className="p-4 text-gray-600 font-medium">{s.players}</td>
                                        <td className="p-4 text-green-600 font-bold">{s.winner || '—'}</td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    )}

                    {activeTab === 'statistics' && (
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                            {Object.entries({
                                'Зіграно ігор': totalGames,
                                'Всього учасників': totalPlayers,
                                'Вікторин у каталозі': quizzes.length,
                            }).map(([label, value]) => (
                                <div key={label} className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
                                    <div className="text-gray-500 text-sm font-bold uppercase tracking-wider mb-2">{label}</div>
                                    <div className="text-4xl font-black text-blue-600">{value}</div>
                                </div>
                            ))}
                        </div>
                    )}

                    {activeTab === 'admin-panel' && role === 'ADMIN' && (
                        <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
                            <table className="w-full text-left border-collapse">
                                <thead><tr className="bg-gray-50 border-b border-gray-200">
                                    <th className="p-4 text-sm font-bold text-gray-600">PIN-код</th>
                                    <th className="p-4 text-sm font-bold text-gray-600">Вікторина</th>
                                    <th className="p-4 text-sm font-bold text-gray-600">Гравців</th>
                                    <th className="p-4 text-sm font-bold text-gray-600">Стан</th>
                                    <th className="p-4 text-sm font-bold text-gray-600 text-right">Дія</th>
                                </tr></thead>
                                <tbody>
                                {sessions.length === 0 && <tr><td colSpan={5} className="p-6 text-center text-gray-400 font-medium">Активних сесій немає</td></tr>}
                                {sessions.map((s) => (
                                    <tr key={s.pin} className="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                                        <td className="p-4 font-bold text-gray-900 tracking-wider">{s.pin}</td>
                                        <td className="p-4 text-gray-700 font-medium">{s.quizTitle}</td>
                                        <td className="p-4 text-gray-700 font-medium">{s.players}</td>
                                        <td className="p-4 text-gray-500 font-medium">{s.state}</td>
                                        <td className="p-4 text-right">
                                            <button onClick={() => handleEndSession(s.pin)} className="bg-red-100 text-red-700 px-4 py-2 rounded-md font-bold text-sm hover:bg-red-600 hover:text-white transition-colors">Завершити примусово</button>
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
                                <thead><tr className="bg-gray-50 border-b border-gray-200">
                                    <th className="p-4 text-sm font-bold text-gray-600">ID</th>
                                    <th className="p-4 text-sm font-bold text-gray-600">Користувач</th>
                                    <th className="p-4 text-sm font-bold text-gray-600">Роль</th>
                                    <th className="p-4 text-sm font-bold text-gray-600">Статус</th>
                                    <th className="p-4 text-sm font-bold text-gray-600 text-right">Дія</th>
                                </tr></thead>
                                <tbody>
                                {users.map((u) => (
                                    <tr key={u.id} className="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                                        <td className="p-4 text-gray-500 font-medium">{u.id}</td>
                                        <td className="p-4 font-bold text-gray-800">@{u.username}</td>
                                        <td className="p-4 text-gray-700 font-medium">{u.role}</td>
                                        <td className="p-4 font-bold">
                                            {u.status === 'ACTIVE' ? <span className="text-green-500">Активний</span> : <span className="text-red-500">Заблокований</span>}
                                        </td>
                                        <td className="p-4 text-right">
                                            <button onClick={() => handleToggleUser(u)} disabled={u.id === user.id}
                                                    className={`px-4 py-2 rounded-md font-bold text-sm transition-colors disabled:opacity-40 ${u.status === 'ACTIVE' ? 'bg-red-100 text-red-700 hover:bg-red-600 hover:text-white' : 'bg-green-100 text-green-700 hover:bg-green-600 hover:text-white'}`}>
                                                {u.status === 'ACTIVE' ? 'Блокувати' : 'Розблокувати'}
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>
            </main>
        </div>
    );
};

const tabClass = (active: boolean) =>
    `text-left px-4 py-2.5 rounded-md font-medium transition-all ${active ? 'bg-blue-50 text-blue-700 shadow-sm' : 'text-gray-600 hover:bg-gray-50'}`;

const titleFor = (tab: string) => ({
    'my-quizzes': 'Мої вікторини',
    'history': 'Історія ігор',
    'statistics': 'Особиста статистика',
    'global-quizzes': 'Глобальний каталог квізів',
    'admin-panel': 'Активні ігрові сесії',
    'users': 'Управління користувачами',
}[tab] || '');

export default Dashboard;
