import { useEffect } from 'react';
import { Outlet, Navigate } from 'react-router-dom';
import { useAuth } from '@/lib/AuthContext';
import UserNotRegisteredError from '@/components/UserNotRegisteredError';

const DefaultFallback = () => (
  <div className="fixed inset-0 flex items-center justify-center">
    <div className="w-8 h-8 border-4 border-slate-200 border-t-slate-800 rounded-full animate-spin"></div>
  </div>
);

export default function ProtectedRoute({ 
  fallback = <DefaultFallback />, 
  unauthenticatedElement,
  allowedRoles
}) {
  const { isAuthenticated, isLoadingAuth, authChecked, authError, user, checkUserAuth } = useAuth();

  useEffect(() => {
    if (!authChecked && !isLoadingAuth) {
      checkUserAuth();
    }
  }, [authChecked, isLoadingAuth, checkUserAuth]);

  if (isLoadingAuth || !authChecked) {
    return fallback;
  }

  if (authError) {
    if (authError.type === 'user_not_registered') {
      return <UserNotRegisteredError />;
    }
    return unauthenticatedElement;
  }

  if (!isAuthenticated) {
    return unauthenticatedElement;
  }

  // Si se especifican roles permitidos y el rol del usuario no está en la lista
  if (allowedRoles && !allowedRoles.includes(user?.role)) {
    const homePorRol = (role) =>
      role === 'admin' ? '/admin'
      : role === 'chofer' ? '/chofer'
      : role === 'usuario' ? '/'
      : '/bienvenida';

    return <Navigate to={homePorRol(user?.role)} replace />;
  }

  return <Outlet />;
}
