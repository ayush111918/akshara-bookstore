import { Route, Routes, useLocation } from 'react-router-dom'
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
import AdminRoute from './components/AdminRoute'
import BookImportPage from './pages/BookImportPage'
import CatalogueAdminPage from './pages/CatalogueAdminPage'
import RouteErrorBoundary from './components/RouteErrorBoundary'
import MyBooksPage from './pages/MyBooksPage'
import AdminOrdersPage from './pages/AdminOrdersPage'
import MyReviewsPage from './pages/MyReviewsPage'
import ReadingJourneyPage from './pages/ReadingJourneyPage'

function App() {
  const location = useLocation()

  return (
    <>
      <Navbar />

      <main className="site-main">
        <RouteErrorBoundary resetKey={location.pathname}>
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
            <Route path="/my-books" element={<ProtectedRoute><MyBooksPage /></ProtectedRoute>} />
            <Route path="/my-reviews" element={<ProtectedRoute><MyReviewsPage /></ProtectedRoute>} />
            <Route path="/reading-journey" element={<ProtectedRoute><ReadingJourneyPage /></ProtectedRoute>} />
            <Route path="/admin/books/import" element={<AdminRoute><BookImportPage /></AdminRoute>} />
            <Route path="/admin/books" element={<AdminRoute><CatalogueAdminPage /></AdminRoute>} />
            <Route path="/admin/orders" element={<AdminRoute><AdminOrdersPage /></AdminRoute>} />
          </Routes>
        </RouteErrorBoundary>
      </main>

      <Footer />
    </>
  )
}

export default App
