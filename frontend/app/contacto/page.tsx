import type { Metadata } from "next";
import { Badge } from "@/components/ui/Badge";
import { Card } from "@/components/ui/Card";
import { Footer } from "@/components/layout/Footer";
import { ContactForm } from "@/components/public/ContactForm";

export const metadata: Metadata = {
  title: "Contacto | OlympusGYM",
  description: "Escríbenos, consulta nuestra dirección y horario de apertura. Gimnasio de demostración, datos de ejemplo.",
  openGraph: {
    title: "Contacto | OlympusGYM",
    description: "Escríbenos, consulta nuestra dirección y horario de apertura.",
  },
};

export default function ContactoPage() {
  return (
    <main>
      <section className="mx-auto max-w-5xl px-4 py-16 sm:px-6">
        <Badge tone="neutral">Datos de ejemplo</Badge>
        <h1 className="mt-4 font-display text-5xl font-extrabold uppercase leading-none tracking-tight sm:text-6xl">
          Hablemos <span className="text-accent-text">claro</span>
        </h1>
        <p className="mt-4 max-w-2xl text-base text-text-muted">
          ¿Dudas sobre tarifas, clases o cómo llegar? Escríbenos y te respondemos en cuanto podamos.
        </p>

        <div className="mt-10 grid grid-cols-1 gap-8 lg:grid-cols-2">
          <Card>
            <h2 className="font-display text-xl font-bold uppercase tracking-tight">Envíanos un mensaje</h2>
            <div className="mt-4">
              <ContactForm />
            </div>
          </Card>

          <div className="flex flex-col gap-6">
            <Card>
              <h2 className="font-display text-xl font-bold uppercase tracking-tight">Dónde estamos</h2>
              <p className="mt-2 text-sm text-text-muted">Calle del Hierro 12, 28045 Madrid (dirección de ejemplo)</p>
              <p className="mt-1 text-sm text-text-muted">Metro Hierro, líneas 5 y 10 (parada de ejemplo)</p>
            </Card>

            <Card>
              <h2 className="font-display text-xl font-bold uppercase tracking-tight">Horario</h2>
              <p className="mt-2 text-sm text-text-muted">Sala de pesas: 06:00 a 23:00, todos los días</p>
              <p className="mt-1 text-sm text-text-muted">Clases dirigidas: según el horario de cada disciplina</p>
            </Card>

            <Card>
              <h2 className="font-display text-xl font-bold uppercase tracking-tight">Contacto directo</h2>
              <p className="mt-2 text-sm text-text-muted">hola@olympusgym.test (correo de ejemplo)</p>
              <p className="mt-1 text-sm text-text-muted">+34 900 000 000 (teléfono de ejemplo)</p>
            </Card>
          </div>
        </div>
      </section>

      <Footer />
    </main>
  );
}
