export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  expiresIn: number;
  userId: string;
  name: string;
  email: string;
}

export interface User {
  id: string;
  name: string;
  email: string;
}
