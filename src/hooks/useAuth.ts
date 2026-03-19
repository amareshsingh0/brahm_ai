import { useAuthStore, type AuthUser } from '@/store/authStore';

function buildMockUser(phone: string): AuthUser {
  return {
    id: 'usr_123',
    name: 'Test User',
    phone,
    plan: 'free',
  };
}

export const useAuth = () => {
  const { setAuth, logout, ...user } = useAuthStore();

  const sendOtp = async (phone: string) => {
    const normalizedPhone = phone.trim();
    // Backend auth routes are not wired yet, so keep the OTP flow stubbed.
    console.log(`Sending OTP to ${normalizedPhone}`);
    return { sent: true };
  };

  const verifyOtp = async (phone: string, otp: string) => {
    const normalizedPhone = phone.trim();
    const normalizedOtp = otp.trim();
    // Backend auth routes are not wired yet, so keep the OTP flow stubbed.
    console.log(`Verifying OTP ${normalizedOtp} for ${normalizedPhone}`);
    const mockUser = buildMockUser(normalizedPhone);
    const mockToken = 'mock-jwt-token';
    setAuth(mockToken, mockUser);
    return { token: mockToken, user: mockUser };
  };

  const userLogout = () => {
    logout();
  };

  return {
    ...user,
    sendOtp,
    verifyOtp,
    logout: userLogout,
  };
};
