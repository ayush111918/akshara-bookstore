import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'

import 'bootstrap/dist/css/bootstrap.min.css'
import 'bootstrap-icons/font/bootstrap-icons.css'
import 'bootstrap/dist/js/bootstrap.bundle.min.js'

import App from './App'
import './index.css'
import './styles/admin-catalogue.css'
import './styles/home.css'
import './styles/catalogue.css'
import './styles/journey-community.css'
import './styles/book-details.css'
import './styles/public-responsive.css'
import './styles/reader-auth.css'
import './styles/commerce.css'
import './styles/my-books.css'
import './styles/admin-orders.css'
import './styles/order-details.css'
import './styles/reviews-discussions.css'
import './styles/community-responsive.css'
import './styles/community-reading-room.css'
import './styles/account-audit.css'
import './styles/reading-journey.css'
import './styles/reading-notebook.css'
import './styles/reading-journey-responsive.css'
import AuthProvider from './providers/AuthProvider'
import ReaderDataProvider from './providers/ReaderDataProvider'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <ReaderDataProvider>
          <App />
        </ReaderDataProvider>
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
)
