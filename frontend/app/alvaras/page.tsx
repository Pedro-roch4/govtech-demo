"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useMutation, useQuery } from "@tanstack/react-query";
import { api, ApiError } from "@/lib/api";
import { useEu } from "@/lib/sessao";
import { STATUS, type Alvara, type Status } from "@/lib/tipos";
import { botao, campo, cartao, erro } from "@/lib/ui";
import Selo from "./selo";

export default function Pedidos() {
  const eu = useEu().data;
  const router = useRouter();
  const [filtro, setFiltro] = useState<Status | "">("");
  const lista = useQuery({
    queryKey: ["alvaras", filtro],
    queryFn: () => api.get<Alvara[]>(`/api/alvaras${filtro ? `?status=${filtro}` : ""}`),
  });
  const novo = useMutation({
    mutationFn: () => api.post<Alvara>("/api/alvaras", {}),
    onSuccess: (a) => router.push(`/alvaras/${a.id}`),
  });
  const cidadao = eu?.papel === "CIDADAO";

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <h1 className="text-xl font-semibold">{cidadao ? "Meus pedidos de alvará" : "Pedidos enviados"}</h1>
        <select
          aria-label="Filtrar por situação"
          className={`${campo} !mt-0 ml-auto w-auto`}
          value={filtro}
          onChange={(e) => setFiltro(e.target.value as Status | "")}
        >
          <option value="">Todas as situações</option>
          {(Object.keys(STATUS) as Status[]).map((s) => (
            <option key={s} value={s}>{STATUS[s].rotulo}</option>
          ))}
        </select>
        {cidadao && (
          <button className={botao} onClick={() => novo.mutate()} disabled={novo.isPending}>
            Novo pedido
          </button>
        )}
      </div>
      {novo.error && <p className={erro}>{novo.error instanceof ApiError ? novo.error.message : "Erro ao criar."}</p>}

      {lista.isPending ? (
        <p className="text-slate-500">Carregando...</p>
      ) : lista.error ? (
        <p className={erro}>Não foi possível carregar os pedidos.</p>
      ) : lista.data.length === 0 ? (
        <div className={`${cartao} text-center text-slate-500`}>Nenhum pedido encontrado.</div>
      ) : (
        <ul className="space-y-2">
          {lista.data.map((a) => (
            <li key={a.id}>
              <Link href={`/alvaras/${a.id}`} className={`${cartao} flex items-center gap-4 hover:border-blue-400`}>
                <span className="w-12 text-sm text-slate-400">#{a.id}</span>
                <span className="flex-1">
                  <span className="block font-medium">{a.nomeEstabelecimento || "Sem nome ainda"}</span>
                  <span className="block text-sm text-slate-500">{a.atividade || "Atividade não informada"}</span>
                </span>
                <Selo status={a.status} />
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
