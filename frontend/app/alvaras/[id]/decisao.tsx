"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { api, ApiError } from "@/lib/api";
import { indeferirSchema, type IndeferirValores } from "@/lib/schemas";
import type { Alvara } from "@/lib/tipos";
import { botao, botaoPerigo, campo, cartao, erro } from "@/lib/ui";

/** Decisao do gestor. Com parecer desfavoravel so o indeferimento e oferecido (o servidor tambem exige isso). */
export default function Decisao({ alvara }: { alvara: Alvara }) {
  const qc = useQueryClient();
  const form = useForm<IndeferirValores>({ resolver: zodResolver(indeferirSchema), defaultValues: { motivo: "" } });
  const decidir = useMutation({
    mutationFn: (corpo: { decisao: "APROVAR" | "INDEFERIR"; motivo?: string }) =>
      api.post<Alvara>(`/api/alvaras/${alvara.id}/homologacao`, corpo),
    onSuccess: (a) => {
      qc.setQueryData(["alvara", String(a.id)], a);
      qc.invalidateQueries({ queryKey: ["alvaras"] });
      qc.invalidateQueries({ queryKey: ["unidades"] });
    },
  });
  const favoravel = alvara.parecer === "FAVORAVEL";

  return (
    <section className={cartao}>
      <h2 className="mb-3 font-semibold">Decisão do gestor</h2>
      {favoravel ? (
        <button className={botao} disabled={decidir.isPending} onClick={() => decidir.mutate({ decisao: "APROVAR" })}>
          Homologar e publicar no mapa
        </button>
      ) : (
        <p className="text-sm text-slate-600">Parecer desfavorável: o pedido só pode ser indeferido.</p>
      )}
      <form
        className="mt-4"
        noValidate
        onSubmit={form.handleSubmit((v) => decidir.mutate({ decisao: "INDEFERIR", motivo: v.motivo }))}
      >
        <label className="text-sm font-medium text-slate-700" htmlFor="motivo">Motivo do indeferimento</label>
        <textarea id="motivo" rows={2} className={campo} {...form.register("motivo")} />
        {form.formState.errors.motivo && <p className={erro}>{form.formState.errors.motivo.message}</p>}
        <button className={`${botaoPerigo} mt-2`} disabled={decidir.isPending}>Indeferir</button>
      </form>
      {decidir.error && (
        <p className={erro}>{decidir.error instanceof ApiError ? decidir.error.message : "Erro ao registrar a decisão."}</p>
      )}
    </section>
  );
}
