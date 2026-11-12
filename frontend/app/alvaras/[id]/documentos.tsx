"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, ApiError } from "@/lib/api";
import type { Documento } from "@/lib/tipos";
import { cartao, erro } from "@/lib/ui";

export default function Documentos({ alvaraId, editavel }: { alvaraId: string; editavel: boolean }) {
  const qc = useQueryClient();
  const chave = ["docs", alvaraId];
  const docs = useQuery({
    queryKey: chave,
    queryFn: () => api.get<Documento[]>(`/api/alvaras/${alvaraId}/documentos`),
  });
  const anexar = useMutation({
    mutationFn: (arquivo: File) => {
      const corpo = new FormData();
      corpo.append("arquivo", arquivo);
      return api.post<Documento>(`/api/alvaras/${alvaraId}/documentos`, corpo);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: chave }),
  });

  return (
    <section className={cartao}>
      <h2 className="mb-3 font-semibold">Documentos</h2>
      {docs.data?.length ? (
        <ul className="divide-y divide-slate-100 text-sm">
          {docs.data.map((d) => (
            <li key={d.id} className="flex items-center gap-3 py-2">
              <a className="text-blue-700 hover:underline" href={`/api/alvaras/${alvaraId}/documentos/${d.id}`}>
                {d.nomeArquivo}
              </a>
              <span className="ml-auto text-slate-500">{Math.max(1, Math.round(d.tamanho / 1024))} KB</span>
            </li>
          ))}
        </ul>
      ) : (
        <p className="text-sm text-slate-500">{docs.isPending ? "Carregando..." : "Nenhum documento anexado."}</p>
      )}
      {editavel && (
        <div className="mt-3">
          <label className="text-sm font-medium text-slate-700" htmlFor="arquivo">
            Anexar documento (PDF, PNG ou JPEG, até 5 MB)
          </label>
          <input
            id="arquivo"
            type="file"
            accept=".pdf,.png,.jpg,.jpeg,application/pdf,image/png,image/jpeg"
            disabled={anexar.isPending}
            className="mt-1 block w-full text-sm file:mr-3 file:rounded-md file:border file:border-slate-300 file:bg-white file:px-3 file:py-1.5"
            onChange={(e) => {
              const arquivo = e.target.files?.[0];
              if (arquivo) anexar.mutate(arquivo);
              e.target.value = "";
            }}
          />
          {anexar.error && (
            <p className={erro}>{anexar.error instanceof ApiError ? anexar.error.message : "Erro ao enviar o arquivo."}</p>
          )}
        </div>
      )}
    </section>
  );
}
