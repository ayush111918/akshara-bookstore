import { useContext } from 'react'
import ReaderDataContext from '../contexts/readerDataContext'

export default function useReaderData() {
  const value = useContext(ReaderDataContext)
  if (!value) throw new Error('useReaderData must be used inside ReaderDataProvider')
  return value
}
