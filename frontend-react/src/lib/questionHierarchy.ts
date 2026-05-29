// Static question-template hierarchy for the drill-down category popup.
// Structure: Category → League → Subject → Team (4 levels)
// Selecting a node that has no children starts a game scoped to that path.
// "Random" at any level pools all terminal descendants.

export interface HierarchyNode {
  id: string;
  name: string;
  children?: HierarchyNode[];
  questionCount?: number;
}

export interface CategoryDefinition {
  id: string;
  name: string;
  description: string;
  theme: string;
  hierarchy: HierarchyNode;
}

const PL_CLUBS: HierarchyNode[] = [
  { id: "arsenal",      name: "Arsenal",        questionCount: 28 },
  { id: "chelsea",      name: "Chelsea",         questionCount: 26 },
  { id: "liverpool",    name: "Liverpool",       questionCount: 30 },
  { id: "man-city",     name: "Manchester City", questionCount: 22 },
  { id: "man-united",   name: "Manchester United", questionCount: 28 },
  { id: "tottenham",    name: "Tottenham",       questionCount: 24 },
  { id: "newcastle",    name: "Newcastle",       questionCount: 18 },
  { id: "aston-villa",  name: "Aston Villa",     questionCount: 16 },
];

const LA_LIGA_CLUBS: HierarchyNode[] = [
  { id: "barcelona",       name: "Barcelona",          questionCount: 24 },
  { id: "real-madrid",     name: "Real Madrid",        questionCount: 26 },
  { id: "atletico-madrid", name: "Atletico Madrid",     questionCount: 20 },
  { id: "valencia",        name: "Valencia",            questionCount: 16 },
  { id: "sevilla",         name: "Sevilla",             questionCount: 14 },
  { id: "villarreal",      name: "Villarreal",          questionCount: 12 },
];

const UCL_CLUBS: HierarchyNode[] = [
  { id: "real-madrid",   name: "Real Madrid",       questionCount: 32 },
  { id: "barcelona",     name: "Barcelona",         questionCount: 28 },
  { id: "bayern-munich", name: "Bayern Munich",     questionCount: 24 },
  { id: "man-city",      name: "Manchester City",   questionCount: 20 },
  { id: "liverpool",     name: "Liverpool",         questionCount: 22 },
  { id: "chelsea",       name: "Chelsea",           questionCount: 18 },
  { id: "juventus",      name: "Juventus",          questionCount: 16 },
  { id: "psg",           name: "Paris Saint-Germain", questionCount: 14 },
];

const SUBJECTS: HierarchyNode[] = [
  { id: "goals",          name: "Goals",            children: [] }, // populated per-league below
  { id: "assists",        name: "Assists",          children: [] },
  { id: "goals-assists",  name: "Goals + Assists",  children: [] },
  { id: "appearances",    name: "Appearances",      children: [] },
];

function cloneWithChildren(name: string, clubs: HierarchyNode[]): HierarchyNode[] {
  return SUBJECTS.map((s) => ({
    ...s,
    children: clubs.map((c) => ({
      ...c,
      id: `${s.id}:${c.id}`,
    })),
  }));
}

const PL_SUBJECTS = cloneWithChildren("Premier League", PL_CLUBS);
const LL_SUBJECTS = cloneWithChildren("La Liga", LA_LIGA_CLUBS);
const CL_SUBJECTS = cloneWithChildren("Champions League", UCL_CLUBS);

export const CATEGORIES: CategoryDefinition[] = [
  {
    id: "football",
    name: "Football",
    description: "Goals, assists & legends of the game",
    theme: "teletext",
    hierarchy: {
      id: "football",
      name: "Football",
      children: [
        {
          id: "premier-league",
          name: "Premier League",
          children: PL_SUBJECTS,
        },
        {
          id: "la-liga",
          name: "La Liga",
          children: LL_SUBJECTS,
        },
        {
          id: "champions-league",
          name: "Champions League",
          children: CL_SUBJECTS,
        },
        {
          id: "serie-a",
          name: "Serie A",
          children: cloneWithChildren("Serie A", [
            { id: "juventus", name: "Juventus", questionCount: 22 },
            { id: "ac-milan", name: "AC Milan", questionCount: 20 },
            { id: "inter", name: "Inter Milan", questionCount: 20 },
            { id: "roma", name: "Roma", questionCount: 16 },
            { id: "napoli", name: "Napoli", questionCount: 14 },
          ]),
        },
        {
          id: "bundesliga",
          name: "Bundesliga",
          children: cloneWithChildren("Bundesliga", [
            { id: "bayern-munich", name: "Bayern Munich", questionCount: 24 },
            { id: "dortmund", name: "Borussia Dortmund", questionCount: 18 },
            { id: "leipzig", name: "RB Leipzig", questionCount: 14 },
            { id: "leverkusen", name: "Bayer Leverkusen", questionCount: 14 },
          ]),
        },
      ],
    },
  },
  {
    id: "geography",
    name: "Geography",
    description: "Populations, capitals & world facts",
    theme: "atlas",
    hierarchy: {
      id: "geography",
      name: "Geography",
      children: [
        { id: "world-populations", name: "World Populations", questionCount: 30 },
        { id: "europe-populations", name: "European Populations", questionCount: 22 },
        { id: "asia-populations", name: "Asian Populations", questionCount: 20 },
        { id: "capitals", name: "World Capitals", questionCount: 40 },
      ],
    },
  },
  {
    id: "music",
    name: "Music",
    description: "Album sales, chart positions & awards",
    theme: "vinyl",
    hierarchy: {
      id: "music",
      name: "Music",
      children: [
        { id: "rock-metal",    name: "Rock & Metal",      questionCount: 18 },
        { id: "pop",           name: "Pop",               questionCount: 18 },
        { id: "hip-hop",       name: "Hip-Hop & R&B",    questionCount: 14 },
        { id: "country",       name: "Country",           questionCount: 12 },
      ],
    },
  },
  {
    id: "film",
    name: "Film",
    description: "Career credits of the silver screen",
    theme: "bigscreen",
    hierarchy: {
      id: "film",
      name: "Film",
      children: [
        { id: "action",     name: "Action",       questionCount: 16 },
        { id: "drama",      name: "Drama",        questionCount: 20 },
        { id: "comedy",     name: "Comedy",       questionCount: 16 },
      ],
    },
  },
];

/**
 * Build a path-based slug for a given drill-down selection.
 * Example: ["football", "premier-league", "goals", "man-city"]
 *       → "football:premier-league:goals:man-city"
 */
export function buildSelectionSlug(path: string[]): string {
  return path.join(":");
}

/**
 * Count total terminal descendants under a node (leaf nodes with no children).
 */
export function countTerminalNodes(node: HierarchyNode): number {
  if (!node.children || node.children.length === 0) return 1;
  return node.children.reduce((sum, child) => sum + countTerminalNodes(child), 0);
}
