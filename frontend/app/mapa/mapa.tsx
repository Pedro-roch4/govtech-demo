"use client";

import { useEffect, useRef } from "react";
import L from "leaflet";
import "leaflet/dist/leaflet.css";

export interface Unidades {
  features: { geometry: { coordinates: [number, number] }; properties: { nome: string; endereco: string } }[];
}

// Centro de Irece (BA); o mapa se ajusta aos pontos quando ha unidades.
const CENTRO: L.LatLngTuple = [-11.3039, -41.8531];

export default function Mapa({ dados }: { dados: Unidades }) {
  const el = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!el.current) return;
    const mapa = L.map(el.current).setView(CENTRO, 14);
    L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
      maxZoom: 19,
      attribution: "&copy; OpenStreetMap",
    }).addTo(mapa);

    // circleMarker evita o problema dos icones padrao do Leaflet com bundlers
    const pontos = dados.features.map((f) => {
      const [lng, lat] = f.geometry.coordinates;
      const p = L.circleMarker([lat, lng], { radius: 9, color: "#1d4ed8", fillColor: "#3b82f6", fillOpacity: 0.85 }).addTo(mapa);
      const popup = document.createElement("div"); // textContent: nada do servidor vira HTML
      const nome = document.createElement("strong");
      nome.textContent = f.properties.nome;
      popup.append(nome, document.createElement("br"), f.properties.endereco);
      p.bindPopup(popup);
      return p;
    });
    if (pontos.length) mapa.fitBounds(L.featureGroup(pontos).getBounds().pad(0.3), { maxZoom: 16 });

    return () => {
      mapa.remove();
    };
  }, [dados]);

  return <div ref={el} className="h-[28rem] w-full rounded-lg border border-slate-200" />;
}
