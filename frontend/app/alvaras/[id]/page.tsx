"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { api, ApiError } from "@/lib/api";
import { useEu } from "@/lib/sessao";
import type { Alvara } from "@/lib/tipos";
import { cartao } from "@/lib/ui";
import Selo from "../selo";
import { FormAnalise, ResultadoAnalise } from "./analise";
import Decisao from "./decisao";
import Documentos from "./documentos";
import Rascunho from "./rascunho";

export default function Pedido() {
  const { id } = useParams<{ id: string }>();
  const eu = useEu().data;
  const q = useQuery({
    queryKey: ["alvara", id],
    queryFn: () => api.get<Alvara>(`/api/alvaras/${id}`),
    retry: false,
  });

  if (q.isPending) return <p className="text-slate-500">Carregando...</p>;
  if (q.error) {
    const naoEncontrado = q.error instanceof ApiError && q.error.status === 404;
    return <p className="text-red-700">{naoEncontrado ? "Pedido não encontrado." : "Não foi possível carregar o pedido."}</p>;
  }
  const a = q.data;
  const rascunho = eu?.papel === "CIDADAO" && a.status === "RASCUNHO";

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-3">
        <Link href="/alvaras" className="text-sm text-blue-700 hover:underline">Voltar</Link>
        <h1 className="text-xl font-semibold">Pedido #{a.id}</h1>
        <Selo status={a.status} />
      </div>

      {rascunho ? (
        <Rascunho alvara={a}>
          <Documentos alvaraId={id} editavel />
        </Rascunho>
      ) : (
        <>
          <section className={cartao}>
            <h2 className="mb-3 font-semibold">Dados do estabelecimento</h2>
            <dl className="grid gap-x-6 gap-y-2 text-sm sm:grid-cols-2">
              <Item rotulo="Nome" valor={a.nomeEstabelecimento} />
              <Item rotulo="Atividade" valor={a.atividade} />
              <Item rotulo="Endereço" valor={a.endereco} />
              <Item rotulo="Localização" valor={a.latitude != null ? `${a.latitude}, ${a.longitude}` : null} />
            </dl>
          </section>
          <Documentos alvaraId={id} editavel={false} />
        </>
      )}

      {a.analise.length > 0 && <ResultadoAnalise alvara={a} />}
      {eu?.papel === "ANALISTA" && a.status === "ENVIADO" && <FormAnalise alvara={a} />}
      {eu?.papel === "GESTOR" && a.status === "ANALISADO" && <Decisao alvara={a} />}

      {a.status === "HOMOLOGADO" && (
        <section className={`${cartao} text-sm`}>
          Pedido homologado. O estabelecimento está publicado no <Link className="text-blue-700 hover:underline" href="/mapa">mapa</Link>.
        </section>
      )}
      {a.status === "INDEFERIDO" && (
        <section className={`${cartao} text-sm`}>
          <span className="font-medium text-red-700">Indeferido.</span> Motivo: {a.motivoDecisao}
        </section>
      )}
    </div>
  );
}

function Item({ rotulo, valor }: { rotulo: string; valor: string | null }) {
  return (
    <div>
      <dt className="text-slate-500">{rotulo}</dt>
      <dd>{valor || "Não informado"}</dd>
    </div>
  );
}
