"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { useEu } from "@/lib/sessao";
import { PAPEIS } from "@/lib/tipos";
import { botaoSec } from "@/lib/ui";

export default function Cabecalho() {
  const eu = useEu().data;
  const router = useRouter();
  const qc = useQueryClient();
  const sair = useMutation({
    mutationFn: () => api.post("/api/auth/logout"),
    onSuccess: () => {
      qc.clear();
      router.push("/login");
    },
  });

  return (
    <header className="border-b border-slate-200 bg-white">
      <div className="mx-auto flex max-w-5xl items-center gap-6 px-4 py-3">
        <Link href="/" className="font-semibold text-blue-800">
          govtech-demo
        </Link>
        <nav className="flex gap-4 text-sm text-slate-600">
          {eu && (
            <Link href="/alvaras" className="hover:text-slate-900">
              Pedidos
            </Link>
          )}
          <Link href="/mapa" className="hover:text-slate-900">
            Mapa
          </Link>
        </nav>
        <div className="ml-auto flex items-center gap-3 text-sm">
          {eu ? (
            <>
              <span className="text-slate-600">
                {eu.nome} <span className="text-slate-400">({PAPEIS[eu.papel]})</span>
              </span>
              <button className={botaoSec} onClick={() => sair.mutate()} disabled={sair.isPending}>
                Sair
              </button>
            </>
          ) : (
            eu === null && (
              <Link href="/login" className={botaoSec}>
                Entrar
              </Link>
            )
          )}
        </div>
      </div>
    </header>
  );
}
