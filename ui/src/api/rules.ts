import client from './client';
import type { Rule } from '../types';

export const listRules = () =>
  client.get<Rule[]>('/rules').then(r => r.data);

export const getRule = (id: number) =>
  client.get<Rule>(`/rules/${id}`).then(r => r.data);

export const createRule = (data: Partial<Rule>) =>
  client.post<Rule>('/rules', data).then(r => r.data);

export const updateRule = (id: number, data: Partial<Rule>) =>
  client.put<Rule>(`/rules/${id}`, data).then(r => r.data);

export const toggleRule = (id: number) =>
  client.patch<Rule>(`/rules/${id}/toggle`).then(r => r.data);

export const deleteRule = (id: number) =>
  client.delete(`/rules/${id}`);
