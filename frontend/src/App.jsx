import { Route, Routes } from 'react-router-dom'
import Footer from './components/Footer'
import Navbar from './components/Navbar'
import CataloguePage from './pages/CataloguePage'
import BookDetailsPage from './pages/BookDetailsPage'
import AuthPage from './pages/AuthPage'
import WishlistPage from './pages/WishlistPage'
import CartPage from './pages/CartPage'
import CheckoutPage from './pages/CheckoutPage'
import OrdersPage from './pages/OrdersPage'
import CommunityPage from './pages/CommunityPage'
import ProtectedRoute from './components/ProtectedRoute'

function App() {
  return (
    <>
      <Navbar />

      <main className="site-main">
        <Routes>
          <Route path="/" element={<CataloguePage />} />
          <Route path="/books/:bookId" element={<BookDetailsPage />} />
          <Route path="/community" element={<CommunityPage />} />
          <Route path="/login" element={<AuthPage mode="login" />} />
          <Route path="/register" element={<AuthPage mode="register" />} />
          <Route path="/wishlist" element={<ProtectedRoute><WishlistPage /></ProtectedRoute>} />
          <Route path="/cart" element={<ProtectedRoute><CartPage /></ProtectedRoute>} />
          <Route path="/checkout" element={<ProtectedRoute><CheckoutPage /></ProtectedRoute>} />
          <Route path="/orders" element={<ProtectedRoute><OrdersPage /></ProtectedRoute>} />
          <Route path="/orders/:orderId" element={<ProtectedRoute><OrdersPage /></ProtectedRoute>} />
        </Routes>
      </main>

      <Footer />
    </>
  )
}

export default App
