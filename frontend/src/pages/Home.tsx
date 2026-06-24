import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import * as React from "react";

const Home = () => {
    const [pin, setPin] = useState('');
    const navigate = useNavigate();

    const handleJoin = (e: React.FormEvent) => {
        e.preventDefault();
        if (pin.trim()) {
            navigate(`/lobby/${pin}`);
        }
    };

    return (
        <div className="min-h-screen bg-blue-600 flex flex-col items-center justify-center p-4">
            <h1 className="text-white text-6xl font-black mb-8 drop-shadow-md">
                KMAhoot!
            </h1>

            <div className="bg-white p-6 rounded-lg shadow-xl w-full max-w-sm">
                <form onSubmit={handleJoin} className="flex flex-col gap-4">
                    <input
                        type="text"
                        placeholder="Введіть PIN гри"
                        value={pin}
                        onChange={(e) => setPin(e.target.value)}
                        className="text-center text-2xl font-bold border-2 border-gray-300 rounded-md p-3 outline-none focus:border-blue-500 transition-colors"
                        maxLength={6}
                    />
                    <button
                        type="submit"
                        className="bg-gray-900 text-white text-xl font-bold py-3 rounded-md hover:bg-gray-800 transition-colors"
                    >
                        Ввійти
                    </button>
                </form>
            </div>

            <div className="mt-5 text-white text-sm">
                Хочете створити свою гру?{' '}
                <button
                    onClick={() => navigate('/authorization')}
                    className="font-bold underline hover:text-gray-200"
                >
                    Авторизуйтесь тут
                </button>
            </div>
        </div>
    );
};

export default Home;