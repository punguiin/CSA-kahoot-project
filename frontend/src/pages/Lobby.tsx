import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';

const Lobby = () => {
    const { pin } = useParams();
    const navigate = useNavigate();
    const [nickname, setNickname] = useState('');
    const [isJoined, setIsJoined] = useState(false);
    const [players, setPlayers] = useState<string[]>(['Микола', 'Назар', 'Нікіта']);

    const handleJoin = (e: React.FormEvent) => {
        e.preventDefault();
        if (nickname.trim()) {
            setPlayers([...players, nickname]);
            setIsJoined(true);
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
                        <button
                            type="submit"
                            className="bg-gray-900 text-white text-xl font-bold py-3 rounded-md hover:bg-gray-800 transition-colors"
                        >
                            Приєднатися
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

            <button
                onClick={() => navigate('/game')}
                className="absolute bottom-10 bg-yellow-400 text-yellow-900 px-8 py-4 rounded-full font-black text-xl shadow-xl border-4 border-yellow-500 hover:bg-yellow-300 hover:scale-105 transition-all"
            >
                DEV: Імітувати старт гри
            </button>
        </div>
    );
};

export default Lobby;