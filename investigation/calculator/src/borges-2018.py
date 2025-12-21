#!/usr/bin/env python3
import csv
import json
from collections import defaultdict

CSV_PATH = '../borges-repos-2018.csv'
ALLOWED_LANGUAGES = {'Java', 'C', 'JavaScript', 'Go', 'Python', 'PHP', 'Ruby'}
TOP_N = 5

def top_by_lang_domain():
    groups = defaultdict(lambda: defaultdict(list))
    with open(CSV_PATH, newline='', encoding='utf-8') as f:
        reader = csv.DictReader(f, delimiter=';')
        for row in reader:
            lang = (row.get('language') or '').strip()
            if not lang or lang not in ALLOWED_LANGUAGES:
                continue
            domain = (row.get('domain') or '').strip() or 'Unknown'
            try:
                stars = int((row.get('stargazers_count') or '0').replace('"','').strip())
            except Exception:
                stars = 0
            full = row.get('full_name').strip()
            lower_full = full.lower().rstrip('/')
            url = f'https://github.com/{lower_full}.git' if lower_full else ''
            groups[lang][domain].append({'name': full, 'stars': stars, 'url': url})

        out = {}
        for lang in sorted(groups.keys()):
            out[lang] = {}
            for domain in sorted(groups[lang].keys()):
                items = sorted(groups[lang][domain], key=lambda x: x['stars'], reverse=True)[:TOP_N]
                out[lang][domain] = [{'name': it['name'], 'stars': it['stars'], 'url': it.get('url', '')} for it in items]
        print(json.dumps(out, ensure_ascii=False, indent=2))
        return

if __name__ == '__main__':
    top_by_lang_domain()