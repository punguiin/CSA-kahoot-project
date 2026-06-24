import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

const Login = () => {
    const [isLoginView, setIsLoginView] = useState(true);
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const navigate = useNavigate();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');

        if (!isLoginView && password !== confirmPassword) {
            setError('Паролі не співпадають');
            return;
        }

        setIsLoading(true);

        try {
            await new Promise(resolve => setTimeout(resolve, 1000));

            if (isLoginView) {
                if (username === 'admin' && password === 'admin') {
                    navigate('/dashboard');
                } else {
                    throw new Error('Неправильний логін або пароль');
                }
            } else {
                navigate('/dashboard');
            }
        } catch (err: any) {
            setError(err.message);
        } finally {
            setIsLoading(false);
        }
    };

    const toggleView = () => {
        setIsLoginView(!isLoginView);
        setError('');
        setUsername('');
        setPassword('');
        setConfirmPassword('');
        setShowPassword(false);
        setShowConfirmPassword(false);
    };

    return (
        <div className="min-h-screen bg-gray-50 flex flex-col justify-center items-center p-4">
            <div className="mb-10 text-center">
                <h1 className="text-4xl font-bold text-blue-600 mb-2">KMAhoot!</h1>
                <p className="text-gray-500">Панель для ведучих та адміністраторів</p>
            </div>

            <div className="bg-white p-8 rounded-xl shadow-lg w-full max-w-md border border-gray-100 transition-all duration-300">
                <h2 className="text-2xl font-bold text-gray-800 mb-6 text-center">
                    {isLoginView ? 'Вхід в акаунт' : 'Створення акаунта'}
                </h2>

                {error && (
                    <div className="mb-4 p-3 bg-red-100 text-red-700 rounded-md text-sm text-center font-medium">
                        {error}
                    </div>
                )}

                <form onSubmit={handleSubmit} className="flex flex-col gap-5">
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                            Ім'я користувача
                        </label>
                        <input
                            type="text"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            className="w-full border border-gray-300 rounded-md p-3 outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-all"
                            required
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                            Пароль
                        </label>
                        <div className="relative">
                            <input
                                type={showPassword ? 'text' : 'password'}
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                className="w-full border border-gray-300 rounded-md p-3 pr-10 outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-all"
                                required
                            />
                            <button
                                type="button"
                                onClick={() => setShowPassword(!showPassword)}
                                className="absolute inset-y-0 right-0 flex items-center pr-3 text-gray-500 hover:text-blue-600"
                            >
                                {showPassword ? (
                                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5">
                                        <path strokeLinecap="round" strokeLinejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88" />
                                    </svg>
                                ) : (
                                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5">
                                        <path strokeLinecap="round" strokeLinejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z" />
                                        <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                                    </svg>
                                )}
                            </button>
                        </div>
                    </div>

                    {!isLoginView && (
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">
                                Повторіть пароль
                            </label>
                            <div className="relative">
                                <input
                                    type={showConfirmPassword ? 'text' : 'password'}
                                    value={confirmPassword}
                                    onChange={(e) => setConfirmPassword(e.target.value)}
                                    className="w-full border border-gray-300 rounded-md p-3 pr-10 outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-all"
                                    required
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                                    className="absolute inset-y-0 right-0 flex items-center pr-3 text-gray-500 hover:text-blue-600"
                                >
                                    {showConfirmPassword ? (
                                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5">
                                            <path strokeLinecap="round" strokeLinejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88" />
                                        </svg>
                                    ) : (
                                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5">
                                            <path strokeLinecap="round" strokeLinejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z" />
                                            <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                                        </svg>
                                    )}
                                </button>
                            </div>
                        </div>
                    )}

                    <button
                        type="submit"
                        disabled={isLoading}
                        className="mt-2 w-full bg-blue-600 text-white font-bold py-3 rounded-md hover:bg-blue-700 transition-colors disabled:bg-blue-400 flex justify-center items-center"
                    >
                        {isLoading ? (
                            <span className="w-6 h-6 border-4 border-white border-t-transparent rounded-full animate-spin"></span>
                        ) : (
                            isLoginView ? 'Увійти' : 'Зареєструватися'
                        )}
                    </button>
                </form>

                <div className="mt-6 text-center text-sm text-gray-600">
                    {isLoginView ? 'Ще не маєте акаунта? ' : 'Вже маєте акаунт? '}
                    <button
                        onClick={toggleView}
                        type="button"
                        className="text-blue-600 font-bold hover:underline"
                    >
                        {isLoginView ? 'Зареєструватися' : 'Увійти'}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default Login;