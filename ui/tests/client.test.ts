import { describe, expect, it, vi } from 'vitest';

const use = vi.fn();
const create = vi.fn(() => ({
  interceptors: {
    response: {
      use,
    },
  },
}));

vi.mock('axios', () => ({
  default: {
    create,
  },
}));

describe('api client', () => {
  it('creates axios client with expected defaults and response interceptor', async () => {
    await import('../src/api/client');

    expect(create).toHaveBeenCalledWith({
      baseURL: '/api',
      headers: { 'Content-Type': 'application/json' },
      timeout: 30000,
    });

    expect(use).toHaveBeenCalledTimes(1);
    const [onSuccess, onError] = use.mock.calls[0];

    const response = { data: { ok: true } };
    expect(onSuccess(response)).toBe(response);

    await expect(onError({ response: { data: { message: 'Backend failed' } } })).rejects.toThrow('Backend failed');
    await expect(onError({ message: 'Network error' })).rejects.toThrow('Network error');
    await expect(onError({})).rejects.toThrow('Request failed');
  });
});
