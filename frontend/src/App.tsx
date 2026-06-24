import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import Home from './pages/Home';
import Lobby from './pages/Lobby';
import Auth from './pages/Auth';
import Dashboard from './pages/Dashboard';
import Game from './pages/Game';
import HostView from './pages/HostView';
import QuizEditor from './pages/QuizEditor';

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
        element: <Dashboard />,
    },
    {
        path: "/game",
        element: <Game />,
    },
    {
        path: "/host/:pin",
        element: <HostView />,
    },
    {
        path: "/create",
        element: <QuizEditor />,
    },
]);

function App() {
    return <RouterProvider router={router} />;
}

export default App;