import axios from 'axios'

const bookApi = axios.create({
  baseURL: '/api/public/books',
  timeout: 10000,
})

export async function getBooks({ query = '', sort = 'TITLE_ASC' } = {}) {
  const params = { sort }

  if (query.trim()) {
    params.query = query.trim()
  }

  const response = await bookApi.get('', { params })
  return response.data
}

export async function getBookById(bookId) {
  const response = await bookApi.get(`/${bookId}`)
  return response.data
}
