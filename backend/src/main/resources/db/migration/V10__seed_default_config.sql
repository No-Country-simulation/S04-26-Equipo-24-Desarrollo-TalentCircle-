-- V8__seed_default_config.sql
-- Seed default pipeline configuration with base prompts for each channel
-- RF-10: Configuración de Prompts

UPDATE pipeline_configs
SET
    llm_provider = 'openai',
    llm_model = 'gpt-4-turbo',
    max_items_per_channel = 10,
    schedule_cron = '0 0 18 * * FRI',
    newsletter_prompt = 'Eres un editor de contenido experto en comunidades de tecnología y talento. '
        || 'Basándote en las siguientes actividades de la comunidad de la semana: {activities} '
        || 'Y los temas principales identificados: {topics} '
        || 'Resumen ejecutivo: {executive_summary} '
        || 'Genera un newsletter en español para el canal {channel} con las siguientes características: '
        || '- Extensión: 800-1200 palabras '
        || '- Incluye: título atractivo, introducción, secciones por tema, CTAs claros '
        || '- Tono: cercano, profesional y motivador '
        || '- Idioma: español',
    linkedin_prompt = 'Eres un experto en marketing de contenidos para LinkedIn. '
        || 'Basándote en las siguientes actividades de la comunidad: {activities} '
        || 'Temas principales: {topics} '
        || 'Resumen: {executive_summary} '
        || 'Genera una publicación de LinkedIn en español con: '
        || '- Extensión: 150-300 palabras '
        || '- Tono: profesional con emojis moderados '
        || '- Incluye: gancho inicial, valor principal, CTA '
        || '- Idioma: español',
    twitter_prompt = 'Eres un experto en comunicación concisa para Twitter/X. '
        || 'Basándote en el resumen de la semana: {executive_summary} '
        || 'Temas: {topics} '
        || 'Genera un tweet en español con: '
        || '- MÁXIMO 280 caracteres (obligatorio) '
        || '- Incluye 2-3 hashtags relevantes '
        || '- Tono: directo e impactante '
        || '- Idioma: español',
    updated_at = CURRENT_TIMESTAMP
WHERE id = '00000000-0000-0000-0000-000000000001';
