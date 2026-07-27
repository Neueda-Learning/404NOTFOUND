import {
  ArrowRight,
  BriefcaseBusiness,
  CalendarDays,
  ChevronDown,
  CircleUserRound,
  Clock3,
  History,
  LogIn,
  Menu,
  Search,
  SlidersHorizontal,
  UserRound,
  UsersRound,
  X,
} from 'lucide-react'

const navGroups = [
  {
    title: 'Customers',
    items: [
      { label: 'Individuals', icon: UserRound },
      { label: 'Organizations', icon: BriefcaseBusiness },
    ],
  },
  {
    title: 'Assessments',
    items: [
      { label: 'Customer', icon: UsersRound, count: 4 },
      { label: 'Real-time', icon: Clock3, count: 6, active: true },
      { label: 'Retro', icon: CalendarDays },
    ],
  },
  {
    title: 'Screening',
    items: [
      { label: 'Scheduled', icon: History },
      { label: 'Initial', icon: LogIn },
    ],
  },
]

const rows = [
  ['22563405', '2023-11-13', 'LTD “Monte”', 'Montenegro', 'UAB “PhoneNam”', 'Lithuania', '15 690.00 EUR', '2023-11-14', '500', '1', 'Resolved', 'Rejected'],
  ['22563399', '2023-11-13', 'Laroda Malimenkovic', 'Montenegro', 'Forda Mifora', 'Montenegro', '27 000.00 EUR', '2023-11-15', '1500', '3', 'Investigating', '—'],
  ['22563393', '2023-11-13', 'UAB “Caputis”', 'Lithuania', 'UAB “PhoneNam”', 'Lithuania', '15 599.99 EUR', '2023-11-14', '500', '1', 'New', '—'],
  ['22563387', '2023-11-13', 'UAB “Caputis”', 'Lithuania', 'UAB “PhoneNam”', 'Lithuania', '15 599.99 EUR', '2023-11-14', '500', '1', 'New', '—'],
  ['22563380', '2023-11-13', 'Arcibald Malimenkov', 'Afghanistan', 'Forda Mifora', 'Montenegro', '25 000.00 EUR', '2023-11-14', '1500', '3', 'Resolved', 'Accepted'],
  ['22563376', '2023-11-13', 'Luka Herald', 'Montenegro', 'LTD “Gyoza”', 'Lithuania', '40 500.99 EUR', '2023-11-14', '500', '1', 'New', '—'],
]

function Sidebar() {
  return (
    <aside className="hidden w-60 shrink-0 bg-sidebar px-3 py-3 lg:block">
      <nav aria-label="Main navigation" className="flex flex-col gap-4">
        {navGroups.map((group) => (
          <section key={group.title} className="flex flex-col gap-1">
            <h2 className="text-xs font-medium text-muted-foreground">{group.title}</h2>
            {group.items.map((item) => {
              const Icon = item.icon
              return (
                <button key={item.label} className={`flex h-8 items-center gap-3 rounded-md px-2 text-left text-sm ${item.active ? 'font-medium text-primary' : 'text-foreground'}`}>
                  <Icon className="size-4" aria-hidden="true" />
                  <span>{item.label}</span>
                  {item.count && <span className="ml-auto rounded-full bg-card px-2 py-0.5 text-xs text-muted-foreground">{item.count}</span>}
                </button>
              )
            })}
          </section>
        ))}
        <section className="flex flex-col gap-2 text-sm">
          <h2 className="text-xs font-medium text-muted-foreground">Leads</h2>
          <button className="px-2 text-left">Cash transaction report</button>
        </section>
      </nav>
    </aside>
  )
}

function Pill({ children }: { children: React.ReactNode }) {
  return <button className="rounded-xl bg-card px-3 py-1.5 text-sm shadow-sm">{children}</button>
}

function RiskPill({ label, tone }: { label: string; tone: string }) {
  return <button className="flex items-center gap-1.5 rounded-xl bg-card px-3 py-1 text-sm"><span className={`size-1.5 rounded-full ${tone}`} />{label}</button>
}

function Filters() {
  return (
    <section aria-label="Assessment filters" className="min-w-[980px] rounded-xl bg-muted p-5">
      <div className="flex gap-5">
        <label className="flex h-10 min-w-0 flex-1 items-center gap-3 rounded-lg border bg-card px-3 text-sm text-muted-foreground">
          <Search className="size-4" aria-hidden="true" />
          <input className="min-w-0 flex-1 bg-transparent outline-none" placeholder="Assessment ID / Op. ext. ID / Customer name or ext. ID / Country" />
        </label>
        <button className="flex h-10 items-center gap-4 rounded-lg border bg-card px-3 text-sm">
          2022-12-04 — 2023-12-04 <CalendarDays className="size-4" />
        </button>
        <button aria-label="Filter settings" className="px-2"><SlidersHorizontal className="size-5" /></button>
      </div>

      <div className="mt-5 grid grid-cols-[230px_1fr_1fr] gap-6">
        <div className="flex flex-col gap-3 border-r pr-5">
          <div className="flex gap-3"><Pill><span className="flex items-center gap-2"><LogIn className="size-4" />Incoming</span></Pill><Pill><span className="flex items-center gap-2"><LogIn className="size-4 rotate-180" />Outgoing</span></Pill></div>
          <button className="flex h-10 items-center justify-between rounded-lg border bg-card px-3 text-sm text-muted-foreground">Operation type <ChevronDown className="size-4 text-primary" /></button>
          <button className="flex h-10 items-center justify-between rounded-lg border bg-card px-3 text-sm text-muted-foreground">Operation category <ChevronDown className="size-4 text-primary" /></button>
          <button className="flex w-fit items-center gap-2 rounded-xl border border-primary/30 bg-card px-3 py-1.5 text-sm text-primary"><X className="size-4" />Clear</button>
        </div>
        <div className="flex flex-col gap-5 border-r pr-5">
          <div><h3 className="mb-3 text-sm text-muted-foreground">Assessment</h3><div className="flex flex-wrap gap-8"><Pill>New</Pill><Pill>Investigating</Pill><Pill>Waiting</Pill><Pill>Resolved</Pill></div></div>
          <div className="flex items-center justify-between"><button className="flex h-10 w-48 items-center justify-between rounded-lg border bg-card px-3 text-sm text-muted-foreground">Decision ground <ChevronDown className="size-4 text-success" /></button><div className="flex gap-2"><Pill>Accepted</Pill><Pill>Rejected</Pill></div></div>
          <div className="mt-6 border-t pt-5"><h3 className="mb-3 text-sm text-muted-foreground">Assessment risk level</h3><div className="flex flex-wrap gap-8"><RiskPill label="None" tone="bg-neutral" /><RiskPill label="Low" tone="bg-success" /><RiskPill label="Medium" tone="bg-warning" /><RiskPill label="High" tone="bg-danger" /><RiskPill label="Extreme" tone="bg-foreground" /></div></div>
        </div>
        <div className="flex flex-col gap-5">
          <div><h3 className="mb-3 text-sm text-muted-foreground">Customer</h3><div className="flex gap-8"><Pill><span className="flex items-center gap-2"><UserRound className="size-4" />Individual</span></Pill><Pill><span className="flex items-center gap-2"><BriefcaseBusiness className="size-4" />Organization</span></Pill></div></div>
          <button className="flex h-10 w-48 items-center justify-between rounded-lg border bg-card px-3 text-sm text-muted-foreground">Customer category <ChevronDown className="size-4 text-primary" /></button>
          <div className="mt-6 border-t pt-5"><h3 className="mb-3 text-sm text-muted-foreground">Customer risk level</h3><div className="flex flex-wrap gap-8"><RiskPill label="None" tone="bg-neutral" /><RiskPill label="Low" tone="bg-success" /><RiskPill label="Medium" tone="bg-warning" /><RiskPill label="High" tone="bg-danger" /><RiskPill label="Extreme" tone="bg-foreground" /></div></div>
        </div>
      </div>
    </section>
  )
}

function AssessmentsTable() {
  return (
    <div className="overflow-x-auto bg-card">
      <table className="w-full min-w-[1220px] border-collapse text-left text-xs">
        <thead><tr className="h-16 border-b text-foreground"><th className="px-5">ID</th><th>• Registration</th><th colSpan={3}>Object</th><th>Amount</th><th>Due</th><th>Score</th><th>Rules count</th><th>Risk level</th><th>Case status</th><th>Decision</th></tr></thead>
        <tbody>{rows.map((row) => <tr key={row[0]} className="h-16 border-b">
          <td className="px-5 underline">{row[0]}</td>
          <td><div className="flex gap-3"><Clock3 className="mt-0.5 size-4" /><div>{row[1]}<small className="block text-muted-foreground">11:06</small></div></div></td>
          <td>{row[2]}<small className="block text-muted-foreground">{row[3]}</small></td>
          <td><ArrowRight className="size-4 text-muted-foreground" /></td>
          <td><div className="flex gap-2"><span className="mt-1 size-3 rounded-full border-2 border-primary" /><div>{row[4]}<small className="block text-muted-foreground">{row[5]}</small></div></div></td>
          <td>{row[6]}</td><td>{row[7]}<small className="block text-muted-foreground">11:06</small></td><td>{row[8]}</td><td>{row[9]}</td>
          <td><span className="inline-flex items-center gap-2"><span className="size-3 rounded-full border-2 border-danger" />High</span></td>
          <td>{row[10]}{row[10] === 'Resolved' && <small className="block text-muted-foreground">2023-11-15 15:56</small>}</td><td>{row[11]}</td>
        </tr>)}</tbody>
      </table>
    </div>
  )
}

export function AssessmentDashboard() {
  return (
    <main className="min-h-screen bg-background text-foreground">
      <header className="flex h-13 items-center bg-brand px-4 text-brand-foreground">
        <Menu className="mr-3 size-5 text-primary" /><span className="text-xl font-bold text-primary">AMLYZE</span>
        <div className="mx-6 h-8 w-px bg-brand-foreground/60" /><h1 className="text-xl font-medium">Assessments</h1>
        <div className="ml-auto flex items-center gap-2 text-sm"><CircleUserRound className="size-7" /><span>Aleksandr Lazutkin</span><ChevronDown className="size-4" /></div>
      </header>
      <div className="flex min-h-[calc(100vh-52px)]"><Sidebar /><div className="min-w-0 flex-1 p-5"><Filters /><AssessmentsTable /></div></div>
    </main>
  )
}
