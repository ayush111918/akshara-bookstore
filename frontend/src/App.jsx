import { Route, Routes } from 'react-router-dom'
import Footer from './components/Footer'
import Navbar from './components/Navbar'
import CataloguePage from './pages/CataloguePage'
import BookDetailsPage from './pages/BookDetailsPage'

function App() {
  return (
    <>
      <Navbar />

      <main className="site-main">
        <Routes>
          <Route path="/" element={<CataloguePage />} />
          <Route path="/books/:bookId" element={<BookDetailsPage />} />
        </Routes>
      </main>

      <Footer />
    </>
  )
}

export default App
