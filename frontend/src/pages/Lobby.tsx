import { useState, useEffect } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { gameClient, MessageType } from '../net/gameClient';

interface RosterPlayer { nickname: string; score: number; }

const Lobby = () => {
    const { pin } = useParams();
    const navigate = useNavigate();
    const location = useLocation();

    const autoUsername = location.state?.username;

    const [nickname, setNickname] = useState(autoUsername || '');
    const [isJoined, setIsJoined] = useState(false);
    const [players, setPlayers] = useState<string[]>([]);
    const [error, setError] = useState('');
    const [isJoining, setIsJoining] = useState(false);

    // Room events flow through the shared client; the QUESTION push means the host started the game.
    useEffect(() => {
        const names = (p: any) => setPlayers((p.players as RosterPlayer[]).map((x) => x.nickname));
        const offJoined = gameClient.on(MessageType.PLAYER_JOINED, names);
        const offLeft = gameClient.on(MessageType.PLAYER_LEFT, names);
        const offQuestion = gameClient.on(MessageType.QUESTION, () => navigate('/game'));
        return () => {
            offJoined();
            offLeft();
            offQuestion();
        };
    }, [navigate]);

    const handleJoin = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!nickname.trim()) return;
        setError('');
        setIsJoining(true);
        try {
            await gameClient.connect();
            const accepted = gameClient.once(MessageType.JOIN_ACCEPTED);
            gameClient.send(MessageType.REQ_JOIN_ROOM, { pin, nickname: nickname.trim() });
            await accepted;
            setIsJoined(true);
        } catch (err: any) {
            setError(err.message || 'Не вдалося приєднатися до кімнати');
        } finally {
            setIsJoining(false);
        }
    };

    if (!isJoined) {
        return (
            <div className="min-h-screen bg-blue-600 flex flex-col items-center justify-center p-4">
                <h1 className="text-white text-4xl font-bold mb-8">Кімната: {pin}</h1>

                <div className="bg-white p-6 rounded-lg shadow-xl w-full max-w-sm">
                    <form onSubmit={handleJoin} className="flex flex-col gap-4">
                        <input
                            type="text"
                            placeholder="Введіть нікнейм"
                            value={nickname}
                            onChange={(e) => setNickname(e.target.value)}
                            className="text-center text-2xl font-bold border-2 border-gray-300 rounded-md p-3 outline-none focus:border-blue-500 transition-colors"
                            maxLength={15}
                        />
                        {error && (
                            <div className="p-3 bg-red-100 text-red-700 rounded-md text-sm text-center font-medium">
                                {error}
                            </div>
                        )}
                        <button
                            type="submit"
                            disabled={isJoining}
                            className="bg-gray-900 text-white text-xl font-bold py-3 rounded-md hover:bg-gray-800 transition-colors disabled:bg-gray-500"
                        >
                            {isJoining ? 'Приєднання...' : 'Приєднатися'}
                        </button>
                    </form>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-blue-600 flex flex-col items-center p-8 relative">
            <div className="w-full flex justify-between items-center mb-12">
                <h2 className="text-white text-3xl font-bold drop-shadow-md">PIN: {pin}</h2>
                <div className="bg-black/20 px-6 py-2 rounded-full text-white font-bold text-xl">
                    Гравців: {players.length}
                </div>
            </div>

            <h1 className="text-white text-5xl font-bold mb-12 animate-pulse text-center drop-shadow-lg">
                Очікуємо ведучого...
            </h1>

            <div className="flex flex-wrap justify-center gap-4 w-full max-w-4xl">
                {players.map((player, index) => (
                    <div
                        key={index}
                        className="bg-white/20 px-6 py-3 rounded-lg text-white text-2xl font-bold shadow-sm"
                    >
                        {player}
                    </div>
                ))}
            </div>
        </div>
    );
};

export default Lobby;
