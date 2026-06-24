import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import Home from './pages/Home';
import Lobby from './pages/Lobby';
import Auth from './pages/Auth';

const router = createBrowserRouter([
    {
        path: "/",
        element: <Home />,
    },
    {
        path: "/lobby/:pin",
        element: <Lobby />,
    },
    {
        path: "/authorization",
        element: <Auth />,
    },
    {
        path: "/dashboard",
        element: <div className="min-h-screen bg-gray-50 p-10 text-2xl font-bold"></div>,
    }
]);

function App() {
    return <RouterProvider router={router} />;
}

export default App;