import { delay, http, HttpResponse } from 'msw';

interface StoredUser {
  id: string;
  nationalId: string;
  fullName: string;
  email: string;
  role: 'FISHER' | 'INSPECTOR' | 'ADMIN';
  password: string;
}

const users: StoredUser[] = [
  { id: 'usr-001', nationalId: '1001001001', fullName: 'Pescador Demo', email: 'pescador@sigepes.gov.co', role: 'FISHER', password: 'Pesca#2026' },
];

export const authHandlers = [
  http.post('/api/v1/auth/login', async ({ request }) => {
    await delay(300);
    const body = (await request.json()) as { email: string; password: string };
    const user = users.find((u) => u.email === body.email && u.password === body.password);
    if (!user) return HttpResponse.json({ message: 'Credenciales inválidas' }, { status: 401 });
    const { password: _pwd, ...safe } = user;
    void _pwd;
    return HttpResponse.json({ accessToken: 'mock-jwt-token', user: safe });
  }),
  http.post('/api/v1/auth/register', async ({ request }) => {
    await delay(350);
    const body = (await request.json()) as Omit<StoredUser, 'id'>;
    if (users.some((u) => u.nationalId === body.nationalId)) {
      return HttpResponse.json({ message: 'La identificación ya está registrada' }, { status: 409 });
    }
    const created: StoredUser = { id: crypto.randomUUID(), ...body };
    users.push(created);
    const { password: _pwd, ...safe } = created;
    void _pwd;
    return HttpResponse.json({ accessToken: 'mock-jwt-token', user: safe }, { status: 201 });
  }),
];
