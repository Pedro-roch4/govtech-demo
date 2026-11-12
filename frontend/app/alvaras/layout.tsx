"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useEu } from "@/lib/sessao";

/** Porteiro das telas de pedidos: sem sessao, vai para o login. A autorizacao de verdade esta no servidor. */
export default function Protegido({ children }: { children: React.ReactNode }) {
  const eu = useEu();
  const router = useRouter();
  useEffect(() => {
    if (eu.data === null) router.replace("/login");
  }, [eu.data, router]);

  if (eu.isError) return <p className="text-red-700">Não foi possível falar com o servidor.</p>;
  if (!eu.data) return <p className="text-slate-500">Carregando...</p>;
  return children;
}
