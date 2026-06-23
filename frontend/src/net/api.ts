import { API_URL } from './config';

export interface AuthUser {
    id: number;
    username: string;
    role: string;
    status: string;
}

const USER_KEY = 'kahoot.user';

export function currentUser(): AuthUser | null {
    try {
        const raw = sessionStorage.getItem(USER_KEY);
        return raw ? (JSON.parse(raw) as AuthUser) : null;
    } catch {
        return null;
    }
}

export function setUser(user: AuthUser): void {
    sessionStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function clearUser(): void {
    sessionStorage.removeItem(USER_KEY);
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    const user = currentUser();
    if (user) {
        headers['X-User-Id'] = String(user.id);
    }
    const res = await fetch(`${API_URL}${path}`, {
        method,
        headers,
        body: body !== undefined ? JSON.stringify(body) : undefined,
    });
    const text = await res.text();
    const data = text ? JSON.parse(text) : null;
    if (!res.ok) {
        throw new Error(data?.message || `HTTP ${res.status}`);
    }
    return data as T;
}

export const api = {
    login: (username: string, password: string) =>
        request<AuthUser>('POST', '/login', { username, password }),
    register: (username: string, password: string) =>
        request<AuthUser>('POST', '/register', { username, password }),
    me: (id: number) => request<AuthUser>('GET', `/me/${id}`),

    quizzes: () => request<any[]>('GET', '/quizzes'),
    quiz: (id: number) => request<any>('GET', `/quizzes/${id}`),
    createQuiz: (quiz: unknown) => request<{ id: number }>('POST', '/quizzes', quiz),
    updateQuiz: (id: number, quiz: unknown) => request<unknown>('PUT', `/quizzes/${id}`, quiz),
    deleteQuiz: (id: number) => request<unknown>('DELETE', `/quizzes/${id}`),

    history: () => request<any[]>('GET', '/history'),

    users: () => request<any[]>('GET', '/users'),
    setUserStatus: (id: number, status: string) =>
        request<unknown>('POST', `/users/${id}/status`, { status }),

    sessions: () => request<any[]>('GET', '/sessions'),
    sessionDetails: (pin: string) => request<any>('GET', `/sessions/${pin}`),
    endSession: (pin: string) => request<unknown>('POST', `/sessions/${pin}/end`),
};
