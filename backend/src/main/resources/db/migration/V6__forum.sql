CREATE TABLE forum_topics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    category VARCHAR(100) NOT NULL DEFAULT 'General',
    created_by_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE forum_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic_id UUID NOT NULL REFERENCES forum_topics(id) ON DELETE CASCADE,
    body TEXT NOT NULL,
    created_by_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

INSERT INTO forum_topics (id, title, body, category, created_by_user_id, created_at) VALUES
('00000000-0000-0000-0000-000000000701','Who wants to discuss Spring AI after the Hall A session?','I am looking for people who want to compare Spring AI agent patterns, MCP usage, and what is realistic for production systems.','Talk discussion','00000000-0000-0000-0000-000000000401',now() - interval '2 hours'),
('00000000-0000-0000-0000-000000000702','Best coffee break topic: JVM performance or AI agents?','I have one break free and want to join a focused discussion. Which topic has more people interested today?','Networking','00000000-0000-0000-0000-000000000412',now() - interval '90 minutes'),
('00000000-0000-0000-0000-000000000703','Looking for architecture lessons from microservices teams','If you have scars from splitting services, observability, ownership, or platform teams, I would love to exchange notes.','Architecture','00000000-0000-0000-0000-000000000410',now() - interval '50 minutes')
ON CONFLICT (id) DO NOTHING;

INSERT INTO forum_comments (id, topic_id, body, created_by_user_id, created_at) VALUES
('00000000-0000-0000-0000-000000000801','00000000-0000-0000-0000-000000000701','I can join after lunch. I am especially interested in how to keep tool access safe for agents.','00000000-0000-0000-0000-000000000411',now() - interval '100 minutes'),
('00000000-0000-0000-0000-000000000802','00000000-0000-0000-0000-000000000701','Count me in. We are trying to connect Spring services with agent workflows at work.','00000000-0000-0000-0000-000000000406',now() - interval '80 minutes'),
('00000000-0000-0000-0000-000000000803','00000000-0000-0000-0000-000000000702','JVM performance has fewer people but deeper discussions. AI agents is easier for a bigger group.','00000000-0000-0000-0000-000000000402',now() - interval '70 minutes'),
('00000000-0000-0000-0000-000000000804','00000000-0000-0000-0000-000000000703','Happy to chat. We learned that team ownership matters more than service size.','00000000-0000-0000-0000-000000000407',now() - interval '30 minutes')
ON CONFLICT (id) DO NOTHING;
