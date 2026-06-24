import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import Home from './pages/Home';
import Lobby from './pages/Lobby';
import Auth from './pages/Auth';
import Dashboard from './pages/Dashboard';

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
    }
]);

function App() {
    return <RouterProvider router={router} />;
}

export default App;