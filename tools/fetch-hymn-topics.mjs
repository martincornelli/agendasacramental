import { mkdir, writeFile } from 'node:fs/promises';
const base = 'https://www.churchofjesuschrist.org/media/music/api-bspservice';
await mkdir('output/hymn-topics', { recursive: true });
for (const lang of ['spa', 'eng']) {
  for (const book of ['hymns', 'hymns-for-home-and-church']) {
    const filters = { bookQueryList: [book], limit: 1000, offset: 0, lang };
    const url = `${base}/song-filtered-list?lang=${lang}&songFilters=${encodeURIComponent(JSON.stringify(filters))}`;
    const response = await fetch(url, {signal: AbortSignal.timeout(120000)});
    if (!response.ok) throw new Error(`${response.status}: ${url}`);
    const data = await response.json();
    if (!Array.isArray(data) || !data.length) throw new Error(`Empty catalog: ${url}`);
    const indexResponse = await fetch(`${base}/song-book?collection=${book}&lang=${lang}`, {signal: AbortSignal.timeout(120000)});
    if (!indexResponse.ok) throw new Error(`Could not verify book index: ${book}`);
    const index = (await indexResponse.json()).data.songList;
    const missing = index.filter(song => !data.some(row => row.slug === song.slug && row.songNumber === song.songNumber));
    if (missing.length) throw new Error(`Incomplete catalog: ${JSON.stringify(missing)}`);
    const records = data.map(({songNumber, title, slug, tags}) => ({number: Number(songNumber), title, slug, topics: tags || []}));
    await writeFile(`output/hymn-topics/${lang}-${book}-topics.json`, JSON.stringify({source: url, records}, null, 2));
    console.log(lang, book, records.length, 'hymns;', records.filter(r => !r.topics.length).length, 'without published topics');
  }
}
