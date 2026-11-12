"use client";

import dynamic from "next/dynamic";
import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { cartao } from "@/lib/ui";
import type { Unidades } from "./mapa";

// Leaflet usa window, entao so carrega no navegador.
const Mapa = dynamic(() => import("./mapa"), { ssr: false });

export default function PaginaMapa() {
  const q = useQuery({ queryKey: ["unidades"], queryFn: () => api.get<Unidades>("/api/unidades") });

  return (
    <div className="space-y-3">
      <h1 className="text-xl font-semibold">Estabelecimentos com alvará homologado</h1>
      {q.error ? (
        <p className="text-red-700">Não foi possível carregar o mapa.</p>
      ) : q.data ? (
        <>
          <Mapa dados={q.data} />
          <p className="text-sm text-slate-500">
            {q.data.features.length === 0
              ? "Ainda não há estabelecimentos homologados."
              : `${q.data.features.length} estabelecimento(s). Dados fictícios.`}
          </p>
        </>
      ) : (
        <div className={`${cartao} text-slate-500`}>Carregando...</div>
      )}
    </div>
  );
}
