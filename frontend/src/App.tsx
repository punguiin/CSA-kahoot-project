import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Home from './pages/Home';

function App() {
  return (
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/lobby/:pin" element={<div className="p-10 text-2xl"></div>} />
          <Route path="/login" element={<div className="p-10 text-2xl"></div>} />
        </Routes>
      </BrowserRouter>
  );
}

export default App;