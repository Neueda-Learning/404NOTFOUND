import client from './client';
import type { AlertListItem, AlertDetail, AlertActionRequest, PageResponse } from '../types';

export interface AlertSearchParams {
  status?: string;
  severity?: string;
  accountId?: string;
  createdFrom?: string;
  createdTo?: string;
  q?: string;
  page?: number;
  size?: number;
}

export const searchAlerts = (params: AlertSearchParams) =>
  client.get<PageResponse<AlertListItem>>('/alerts', { params }).then(r => r.data);

export const getAlert = (id: string) =>
  client.get<AlertDetail>(`/alerts/${id}`).then(r => r.data);

export const acknowledgeAlert = (id: string, req: AlertActionRequest) =>
  client.post<AlertDetail>(`/alerts/${id}/acknowledge`, req).then(r => r.data);

export const investigateAlert = (id: string, req: AlertActionRequest) =>
  client.post<AlertDetail>(`/alerts/${id}/investigate`, req).then(r => r.data);

export const closeAlert = (id: string, req: AlertActionRequest) =>
  client.post<AlertDetail>(`/alerts/${id}/close`, req).then(r => r.data);

export const dismissAlert = (id: string, req: AlertActionRequest) =>
  client.post<AlertDetail>(`/alerts/${id}/dismiss`, req).then(r => r.data);
