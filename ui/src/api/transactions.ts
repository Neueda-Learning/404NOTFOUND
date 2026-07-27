import client from './client';
import type { Transaction, PageResponse } from '../types';

export interface TransactionSearchParams {
  accountId?: string;
  payeeId?: string;
  status?: string;
  type?: string;
  fromTime?: string;
  toTime?: string;
  q?: string;
  page?: number;
  size?: number;
}

export const searchTransactions = (params: TransactionSearchParams) =>
  client.get<PageResponse<Transaction>>('/transactions', { params }).then(r => r.data);

export const getTransaction = (id: string) =>
  client.get<Transaction>(`/transactions/${id}`).then(r => r.data);
