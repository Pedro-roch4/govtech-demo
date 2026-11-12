"use client";

import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { api, ApiError } from "@/lib/api";
import { rascunhoSchema, type RascunhoValores } from "@/lib/schemas";
import type { Alvara } from "@/lib/tipos";
import { botao, campo, cartao, erro, rotulo } from "@/lib/ui";

const numero = { setValueAs: (v: string) => (v === "" || v == null ? undefined : Number(v)) };

/** Formulario do rascunho com autosave (debounce) e envio. Recebe os documentos como filhos para ficarem entre os dois. */
export default function Rascunho({ alvara, children }: { alvara: Alvara; children: React.ReactNode }) {
  const qc = useQueryClient();
  const url = `/api/alvaras/${alvara.id}`;
  const [salvoEm, setSalvoEm] = useState<Date | null>(null);
  const form = useForm<RascunhoValores>({
    resolver: zodResolver(rascunhoSchema),
    mode: "onChange",
    defaultValues: {
      nomeEstabelecimento: alvara.nomeEstabelecimento ?? "",
      atividade: alvara.atividade ?? "",
      endereco: alvara.endereco ?? "",
      latitude: alvara.latitude ?? undefined,
      longitude: alvara.longitude ?? undefined,
    },
  });

  const salvar = useMutation({
    mutationFn: (v: RascunhoValores) => api.put<Alvara>(url, v),
    onSuccess: (a) => {
      qc.setQueryData(["alvara", String(a.id)], a);
      setSalvoEm(new Date());
    },
  });
  const { mutate } = salvar;

  // Autosave: espera 800 ms sem digitar e so salva se os campos estiverem validos.
  useEffect(() => {
    let t: ReturnType<typeof setTimeout>;
    const cancelar = form.subscribe({
      formState: { values: true },
      callback: () => {
        clearTimeout(t);
        t = setTimeout(async () => {
          if (await form.trigger()) mutate(form.getValues());
        }, 800);
      },
    });
    return () => {
      cancelar();
      clearTimeout(t);
    };
  }, [form, mutate]);

  const enviar = useMutation({
    mutationFn: async () => {
      if (!(await form.trigger())) throw new ApiError(400, "Corrija os campos destacados.");
      await api.put(url, form.getValues()); // garante que o servidor tem o que esta na tela
      return api.post<Alvara>(`${url}/enviar`);
    },
    onSuccess: (a) => {
      qc.setQueryData(["alvara", String(a.id)], a);
      qc.invalidateQueries({ queryKey: ["alvaras"] });
    },
  });

  const { errors } = form.formState;
  const msg = (e: Error | null) => (e ? (e instanceof ApiError ? e.message : "Erro de conexão.") : null);

  return (
    <>
      <section className={cartao}>
        <div className="mb-3 flex items-baseline gap-3">
          <h2 className="font-semibold">Dados do estabelecimento</h2>
          <span className="ml-auto text-xs text-slate-500" aria-live="polite">
            {salvar.isPending
              ? "Salvando..."
              : salvoEm
                ? `Rascunho salvo às ${salvoEm.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" })}`
                : "O rascunho é salvo automaticamente"}
          </span>
        </div>
        <form className="grid gap-3 sm:grid-cols-2" noValidate onSubmit={(e) => e.preventDefault()}>
          <div>
            <label className={rotulo} htmlFor="nome">Nome do estabelecimento</label>
            <input id="nome" className={campo} {...form.register("nomeEstabelecimento")} />
            {errors.nomeEstabelecimento && <p className={erro}>{errors.nomeEstabelecimento.message}</p>}
          </div>
          <div>
            <label className={rotulo} htmlFor="atividade">Atividade</label>
            <input id="atividade" className={campo} {...form.register("atividade")} />
            {errors.atividade && <p className={erro}>{errors.atividade.message}</p>}
          </div>
          <div className="sm:col-span-2">
            <label className={rotulo} htmlFor="endereco">Endereço</label>
            <input id="endereco" className={campo} {...form.register("endereco")} />
            {errors.endereco && <p className={erro}>{errors.endereco.message}</p>}
          </div>
          <div>
            <label className={rotulo} htmlFor="lat">Latitude</label>
            <input id="lat" type="number" step="any" className={campo} {...form.register("latitude", numero)} />
            {errors.latitude && <p className={erro}>{errors.latitude.message}</p>}
          </div>
          <div>
            <label className={rotulo} htmlFor="lng">Longitude</label>
            <input id="lng" type="number" step="any" className={campo} {...form.register("longitude", numero)} />
            {errors.longitude && <p className={erro}>{errors.longitude.message}</p>}
          </div>
        </form>
        {msg(salvar.error) && <p className={erro}>{msg(salvar.error)}</p>}
      </section>

      {children}

      <div className="flex items-center gap-3">
        <button className={botao} onClick={() => enviar.mutate()} disabled={enviar.isPending}>
          Enviar para análise
        </button>
        <span className="text-sm text-slate-500">Exige todos os dados preenchidos e ao menos um documento.</span>
      </div>
      {msg(enviar.error) && <p className={erro}>{msg(enviar.error)}</p>}
    </>
  );
}
