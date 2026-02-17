export interface User {
  userId: number;
  nickname: string;
}

export interface GuestLoginResponse extends User {}
