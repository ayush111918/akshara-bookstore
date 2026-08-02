import { Route, Routes } from 'react-router-dom'
import Navbar from './components/Navbar'
import CataloguePage from './pages/CataloguePage'
import BookDetailsPage from './pages/BookDetailsPage'

function App() {
  return (
    <>
      <Navbar />

      <main>
        <Routes>
          <Route path="/" element={<CataloguePage />} />
          <Route path="/books/:bookId" element={<BookDetailsPage />} />
        </Routes>
      </main>
    </>
  )
}

export default App