"use client";

import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { api, ApiError } from "@/lib/api";
import { analiseSchema, type AnaliseValores } from "@/lib/schemas";
import { CRITERIOS, type Alvara, type Criterio, type ResultadoCriterio } from "@/lib/tipos";
import { botao, campo, cartao, erro } from "@/lib/ui";

/** Resultado ja registrado (somente leitura). */
export function ResultadoAnalise({ alvara }: { alvara: Alvara }) {
  return (
    <section className={cartao}>
      <div className="mb-3 flex items-baseline gap-3">
        <h2 className="font-semibold">Análise por critérios</h2>
        <span className={`text-sm font-medium ${alvara.parecer === "FAVORAVEL" ? "text-emerald-700" : "text-red-700"}`}>
          Parecer {alvara.parecer === "FAVORAVEL" ? "favorável" : "desfavorável"}
        </span>
      </div>
      <ul className="space-y-2 text-sm">
        {alvara.analise.map((r: ResultadoCriterio) => (
          <li key={r.criterio}>
            <span className={r.atendido ? "text-emerald-700" : "text-red-700"}>{r.atendido ? "Atendido" : "Não atendido"}</span>
            {": "}
            {CRITERIOS[r.criterio]}
            {r.observacao && <span className="block text-slate-500">Observação: {r.observacao}</span>}
          </li>
        ))}
      </ul>
    </section>
  );
}

/** Formulario do analista: avalia os 4 criterios; o que nao for atendido exige observacao. */
export function FormAnalise({ alvara }: { alvara: Alvara }) {
  const qc = useQueryClient();
  const form = useForm<AnaliseValores>({
    resolver: zodResolver(analiseSchema),
    defaultValues: {
      criterios: (Object.keys(CRITERIOS) as Criterio[]).map((criterio) => ({ criterio, atendido: true, observacao: "" })),
    },
  });
  const analisar = useMutation({
    mutationFn: (v: AnaliseValores) =>
      api.post<Alvara>(`/api/alvaras/${alvara.id}/analise`, {
        criterios: v.criterios.map((c) => ({ ...c, observacao: c.observacao.trim() || null })),
      }),
    onSuccess: (a) => {
      qc.setQueryData(["alvara", String(a.id)], a);
      qc.invalidateQueries({ queryKey: ["alvaras"] });
    },
  });
  const atendidos = useWatch({ control: form.control, name: "criterios" });
  const { errors } = form.formState;

  return (
    <section className={cartao}>
      <h2 className="mb-3 font-semibold">Análise por critérios</h2>
      <form onSubmit={form.handleSubmit((v) => analisar.mutate(v))} noValidate className="space-y-4">
        {(Object.keys(CRITERIOS) as Criterio[]).map((c, i) => (
          <div key={c}>
            <label className="flex items-center gap-2 text-sm font-medium">
              <input type="checkbox" {...form.register(`criterios.${i}.atendido`)} />
              {CRITERIOS[c]}
            </label>
            {!atendidos[i]?.atendido && (
              <>
                <input
                  className={campo}
                  placeholder="Observação (obrigatória quando não atendido)"
                  aria-label={`Observação sobre ${CRITERIOS[c]}`}
                  {...form.register(`criterios.${i}.observacao`)}
                />
                {errors.criterios?.[i]?.observacao && <p className={erro}>{errors.criterios[i]?.observacao?.message}</p>}
              </>
            )}
          </div>
        ))}
        <p className="text-sm text-slate-500">Basta um critério não atendido para o parecer ser desfavorável.</p>
        {analisar.error && (
          <p className={erro}>{analisar.error instanceof ApiError ? analisar.error.message : "Erro ao registrar."}</p>
        )}
        <button className={botao} disabled={analisar.isPending}>Registrar análise</button>
      </form>
    </section>
  );
}
