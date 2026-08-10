import React from "react";

const LOGO_URL = '/icon-mark.png';

export default function AuthLayout({ title, subtitle, footer, children }) {
  return (
    <div className="min-h-screen flex items-center justify-center bg-[#05090f] px-4">
      <div className="w-full max-w-md">
        <div className="text-center mb-10">
          <img src={LOGO_URL} alt="NEXVIA" className="h-20 w-auto mx-auto rounded-2xl mb-4 object-contain" />
          <h1 className="text-3xl font-bold tracking-tight text-white">{title}</h1>
          {subtitle && <p className="text-stone-400 mt-2">{subtitle}</p>}
        </div>
        <div className="bg-white rounded-2xl shadow-2xl p-8">
          {children}
        </div>
        {footer && (
          <p className="text-center text-sm text-stone-500 mt-6">{footer}</p>
        )}
      </div>
    </div>
  );
}