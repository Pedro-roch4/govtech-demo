export type Papel = "CIDADAO" | "ANALISTA" | "GESTOR";
export type Status = "RASCUNHO" | "ENVIADO" | "ANALISADO" | "HOMOLOGADO" | "INDEFERIDO";
export type Criterio =
  | "DOCUMENTACAO_COMPLETA"
  | "ENDERECO_COMPATIVEL_COM_ZONEAMENTO"
  | "SEM_PENDENCIAS_FISCAIS"
  | "VISTORIA_APROVADA";

export interface Usuario {
  id: number;
  nome: string;
  email: string;
  papel: Papel;
}

export interface ResultadoCriterio {
  criterio: Criterio;
  atendido: boolean;
  observacao: string | null;
}

export interface Alvara {
  id: number;
  versao: number;
  status: Status;
  parecer: "FAVORAVEL" | "DESFAVORAVEL" | null;
  nomeEstabelecimento: string | null;
  atividade: string | null;
  endereco: string | null;
  latitude: number | null;
  longitude: number | null;
  motivoDecisao: string | null;
  analise: ResultadoCriterio[];
  criadoEm: string;
  atualizadoEm: string;
}

export interface Documento {
  id: number;
  nomeArquivo: string;
  contentType: string;
  tamanho: number;
  enviadoEm: string;
}

export const CRITERIOS: Record<Criterio, string> = {
  DOCUMENTACAO_COMPLETA: "Documentação completa",
  ENDERECO_COMPATIVEL_COM_ZONEAMENTO: "Endereço compatível com o zoneamento",
  SEM_PENDENCIAS_FISCAIS: "Sem pendências fiscais",
  VISTORIA_APROVADA: "Vistoria aprovada",
};

export const STATUS: Record<Status, { rotulo: string; cor: string }> = {
  RASCUNHO: { rotulo: "Rascunho", cor: "bg-slate-200 text-slate-700" },
  ENVIADO: { rotulo: "Enviado", cor: "bg-blue-100 text-blue-800" },
  ANALISADO: { rotulo: "Analisado", cor: "bg-amber-100 text-amber-800" },
  HOMOLOGADO: { rotulo: "Homologado", cor: "bg-emerald-100 text-emerald-800" },
  INDEFERIDO: { rotulo: "Indeferido", cor: "bg-red-100 text-red-800" },
};

export const PAPEIS: Record<Papel, string> = {
  CIDADAO: "Cidadão",
  ANALISTA: "Analista",
  GESTOR: "Gestor",
};
