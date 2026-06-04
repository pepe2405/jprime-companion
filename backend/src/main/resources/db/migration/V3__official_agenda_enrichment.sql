ALTER TABLE talks ADD COLUMN IF NOT EXISTS official_id INTEGER;
ALTER TABLE talks ADD COLUMN IF NOT EXISTS talk_level VARCHAR(50);
ALTER TABLE talks ADD COLUMN IF NOT EXISTS beginner_friendly BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE talks ADD COLUMN IF NOT EXISTS format VARCHAR(100);
ALTER TABLE talks ADD COLUMN IF NOT EXISTS audience VARCHAR(255);
ALTER TABLE talks ADD COLUMN IF NOT EXISTS takeaways TEXT[];
ALTER TABLE talks ADD COLUMN IF NOT EXISTS source_url VARCHAR(500);

UPDATE talks SET
    official_id = 254,
    title = 'Agentic AI Patterns for Enterprise Software',
    speaker = 'Kevin Dubois',
    description = 'Explore the spectrum of agentic patterns, from reliable workflows to autonomous agent orchestration, with Java, LangChain4j, Quarkus, and enterprise collaboration patterns.',
    hall = 'Hall A',
    start_time = '2026-06-03 11:05',
    end_time = '2026-06-03 11:55',
    tags = ARRAY['AI Agents','Architecture','Java','Quarkus'],
    talk_level = 'INTERMEDIATE',
    beginner_friendly = false,
    format = 'Technical session',
    audience = 'Backend engineers and architects evaluating agentic AI for enterprise systems.',
    takeaways = ARRAY['Compare agentic workflow patterns','Understand orchestration trade-offs','See Java-oriented implementation ideas'],
    source_url = 'https://jprime.io/agenda/254'
WHERE id = '00000000-0000-0000-0000-000000000301';

UPDATE talks SET
    official_id = 259,
    title = 'Building production-ready AI Agents with Spring AI and Amazon Bedrock AgentCore',
    speaker = 'Vadym Kazulkin',
    description = 'Dive into implementing Java AI agents with Spring AI, MCP Streamable HTTP clients, Amazon Bedrock AgentCore, observability, gateway, memory, and identity.',
    hall = 'Hall B',
    start_time = '2026-06-03 14:05',
    end_time = '2026-06-03 14:55',
    tags = ARRAY['Spring','AI Agents','Java','AWS'],
    talk_level = 'INTERMEDIATE',
    beginner_friendly = false,
    format = 'Technical session',
    audience = 'Java and Spring developers preparing AI agents for production.',
    takeaways = ARRAY['Understand Spring AI agent deployment','Learn AgentCore production capabilities','Connect agents to MCP-compatible tools'],
    source_url = 'https://jprime.io/agenda/259'
WHERE id = '00000000-0000-0000-0000-000000000302';

UPDATE talks SET
    official_id = 253,
    title = 'Practical MCP Security in Action',
    speaker = 'Willem Jan Glerum',
    description = 'Learn how MCP clients and servers can securely authorize access to tools, prompts, and resources using OAuth2, dynamic client registration, and Quarkus MCP components.',
    hall = 'Hall B',
    start_time = '2026-06-03 10:00',
    end_time = '2026-06-03 10:50',
    tags = ARRAY['Security','AI Agents','MCP','Quarkus'],
    talk_level = 'BEGINNER',
    beginner_friendly = true,
    format = 'Technical session',
    audience = 'Developers new to MCP security and secure AI tool integration.',
    takeaways = ARRAY['Understand MCP authorization','See secure OAuth2 client flows','Learn MCP server security considerations'],
    source_url = 'https://jprime.io/agenda/253'
WHERE id = '00000000-0000-0000-0000-000000000303';

UPDATE talks SET
    official_id = 257,
    title = 'Kotlin for Normal Brains (Without Jets)',
    speaker = 'Nayden Gochev',
    description = 'A practical introduction to Kotlin for Java developers: null-safety, data classes, extensions, smart casts, functions, lambdas, collections, and migration without rewriting everything.',
    hall = 'Hall B',
    start_time = '2026-06-03 13:00',
    end_time = '2026-06-03 13:50',
    tags = ARRAY['Kotlin','Java','JVM'],
    talk_level = 'BEGINNER',
    beginner_friendly = true,
    format = 'Technical session',
    audience = 'Java developers curious about Kotlin as a complementary JVM skill.',
    takeaways = ARRAY['Understand Kotlin basics from a Java perspective','See how Kotlin reduces boilerplate','Learn migration-friendly Kotlin concepts'],
    source_url = 'https://jprime.io/agenda/257'
WHERE id = '00000000-0000-0000-0000-000000000304';

UPDATE talks SET
    official_id = 263,
    title = 'Observing Project Valhalla',
    speaker = 'Cay Horstmann',
    description = 'Learn the current state of Project Valhalla and how to observe its impact using JMH benchmarks, Java Flight Recorder, JITWatch, Java Object Layout, and related tools.',
    hall = 'Hall A',
    start_time = '2026-06-03 16:25',
    end_time = '2026-06-03 17:15',
    tags = ARRAY['Java','JVM Performance','Project Valhalla'],
    talk_level = 'ADVANCED',
    beginner_friendly = false,
    format = 'Technical session',
    audience = 'Experienced Java developers evaluating JVM and language-level performance changes.',
    takeaways = ARRAY['Understand value types','Measure Valhalla behavior','Apply JVM observability tools'],
    source_url = 'https://jprime.io/agenda/263'
WHERE id = '00000000-0000-0000-0000-000000000305';

UPDATE talks SET
    official_id = 255,
    title = 'JDK 25''s new CPU-Time Profiler',
    speaker = 'Johannes Bechberger',
    description = 'An introduction to JDK 25 CPU-time profiling, why execution-time profiles can mislead, and how the new profiler helps focus on actual CPU bottlenecks.',
    hall = 'Hall B',
    start_time = '2026-06-03 11:05',
    end_time = '2026-06-03 11:55',
    tags = ARRAY['Java','JVM Performance','Profiling'],
    talk_level = 'INTERMEDIATE',
    beginner_friendly = false,
    format = 'Technical session',
    audience = 'Java developers who profile throughput, latency, or CPU bottlenecks.',
    takeaways = ARRAY['Understand CPU-time profiling','Avoid misleading execution profiles','Use JDK 25 profiler foundations'],
    source_url = 'https://jprime.io/agenda/255'
WHERE id = '00000000-0000-0000-0000-000000000306';

UPDATE talks SET
    official_id = 252,
    title = 'Learning modern Java the playful way',
    speaker = 'Piotr Przybył && Marit van Dijk',
    description = 'A friendly introduction to modern Java features such as structured concurrency and pattern matching, with IDE-oriented examples you can use the next day.',
    hall = 'Hall A',
    start_time = '2026-06-03 10:00',
    end_time = '2026-06-03 10:50',
    tags = ARRAY['Java','Modern Java','IDE'],
    talk_level = 'BEGINNER',
    beginner_friendly = true,
    format = 'Technical session',
    audience = 'Developers new to recent Java versions or returning after Java 8.',
    takeaways = ARRAY['Learn modern Java features','See IDE support in practice','Start applying features safely'],
    source_url = 'https://jprime.io/agenda/252'
WHERE id = '00000000-0000-0000-0000-000000000307';

UPDATE talks SET
    official_id = 256,
    title = 'Experiment Agentic AI patterns (and MCP) within your own workloads',
    speaker = 'Arnaud Jean',
    description = 'Explore design frameworks and implementation approaches for controllable AI agent systems with Spring AI, code samples, and live demo resources.',
    hall = 'Hall A',
    start_time = '2026-06-03 13:00',
    end_time = '2026-06-03 13:50',
    tags = ARRAY['Spring','AI Agents','MCP','Architecture'],
    talk_level = 'INTERMEDIATE',
    beginner_friendly = false,
    format = 'Technical session',
    audience = 'Developers experimenting with agentic systems and Spring AI.',
    takeaways = ARRAY['Choose agent communication patterns','Keep agent systems controllable','Apply Spring AI examples'],
    source_url = 'https://jprime.io/agenda/256'
WHERE id = '00000000-0000-0000-0000-000000000308';

UPDATE talks SET
    official_id = 261,
    title = 'Agents With Seatbelts: Practical Ways to Keep AI Code Gen Under Control',
    speaker = 'Jonathan Vila López',
    description = 'Best practices for MCP setups and AI agents in real development workflows, focusing on quality, security, validation, permissions, and realistic productivity gains.',
    hall = 'Hall A',
    start_time = '2026-06-03 15:20',
    end_time = '2026-06-03 16:10',
    tags = ARRAY['AI Agents','Security','Developer Productivity'],
    talk_level = 'INTERMEDIATE',
    beginner_friendly = false,
    format = 'Technical session',
    audience = 'Teams adopting AI coding tools and MCP workflows.',
    takeaways = ARRAY['Tighten AI tool access','Add validation steps','Design safer agent workflows'],
    source_url = 'https://jprime.io/agenda/261'
WHERE id = '00000000-0000-0000-0000-000000000309';

UPDATE talks SET
    official_id = 262,
    title = 'Never a Null Moment with JSpecify',
    speaker = 'Hinse ter Schuur',
    description = 'Learn how JSpecify standardizes nullness semantics for Java and how to introduce precise null-safety incrementally in production code.',
    hall = 'Hall B',
    start_time = '2026-06-03 15:20',
    end_time = '2026-06-03 16:10',
    tags = ARRAY['Java','Testing','Code Quality'],
    talk_level = 'INTERMEDIATE',
    beginner_friendly = false,
    format = 'Technical session',
    audience = 'Java developers improving code safety and library contracts.',
    takeaways = ARRAY['Understand JSpecify semantics','Reduce null-related ambiguity','Adopt nullness incrementally'],
    source_url = 'https://jprime.io/agenda/262'
WHERE id = '00000000-0000-0000-0000-000000000310';

UPDATE talks SET
    official_id = 264,
    title = 'Automating Workflows with Multi-Agent Systems',
    speaker = 'Kristiyan Stoyanov',
    description = 'Understand why single-agent systems fail unpredictably and how multi-agent architectures model workflows through coordinated agent interactions and agent-to-agent collaboration.',
    hall = 'Hall B',
    start_time = '2026-06-03 16:25',
    end_time = '2026-06-03 17:15',
    tags = ARRAY['AI Agents','Architecture','Backend'],
    talk_level = 'INTERMEDIATE',
    beginner_friendly = false,
    format = 'Technical session',
    audience = 'Backend engineers designing practical multi-agent workflows.',
    takeaways = ARRAY['Recognize single-agent limits','Model agent collaboration','Think about backend integration'],
    source_url = 'https://jprime.io/agenda/264'
WHERE id = '00000000-0000-0000-0000-000000000311';

UPDATE talks SET
    official_id = 266,
    title = 'Trash Talk - Exploring the JVM memory management',
    speaker = 'Gerrit Grunwald',
    description = 'A practical tour of JVM memory allocation, object lifecycle, garbage collection strategies, and how to choose the right garbage collector for your application.',
    hall = 'Hall A',
    start_time = '2026-06-03 17:30',
    end_time = '2026-06-03 18:20',
    tags = ARRAY['Java','JVM Performance','Garbage Collection'],
    talk_level = 'BEGINNER',
    beginner_friendly = true,
    format = 'Technical session',
    audience = 'Java developers who want stronger JVM memory-management fundamentals.',
    takeaways = ARRAY['Understand JVM memory basics','Compare garbage collectors','Improve performance decisions'],
    source_url = 'https://jprime.io/agenda/266'
WHERE id = '00000000-0000-0000-0000-000000000312';

INSERT INTO talks (id, official_id, title, speaker, description, hall, start_time, end_time, tags, talk_level, beginner_friendly, format, audience, takeaways, source_url, created_at, updated_at) VALUES
('00000000-0000-0000-0000-000000000313',267,'How Coding Agents Are Transforming Software Development: From Code Completion to Autonomous Feature Delivery','Ioannis Kolaxis','A clear view of the opportunities and risks when AI coding agents move from code completion to planning, changing files, running checks, and delivering features.','Hall B','2026-06-03 17:30','2026-06-03 18:20',ARRAY['AI Agents','Developer Productivity','Testing'],'BEGINNER',true,'Technical session','Developers and tech leads adapting to agent-based development.',ARRAY['Understand agent-based development','Know quality risks','Improve specs and tests'],'https://jprime.io/agenda/267',now(),now()),
('00000000-0000-0000-0000-000000000314',268,'Know Your Java?','Venkat Subramaniam','An interactive session on surprising Java behavior and deeper language understanding for the code developers use every day.','Hall A','2026-06-04 10:00','2026-06-04 10:50',ARRAY['Java','Language Fundamentals'],'BEGINNER',true,'Interactive session','Java developers at any level who enjoy language puzzles.',ARRAY['Spot Java surprises','Deepen language understanding','Discuss code behavior interactively'],'https://jprime.io/agenda/268',now(),now()),
('00000000-0000-0000-0000-000000000315',270,'Building Agents with Spring AI','Sergi Almar','Build progressively more capable AI agents with Spring AI, from tool calling to workflows, recursive advisors, MCP, and A2A collaboration.','Hall A','2026-06-04 11:05','2026-06-04 11:55',ARRAY['Spring','AI Agents','MCP'],'BEGINNER',true,'Technical session','Spring developers who want to start building agentic applications.',ARRAY['Build Spring AI agents','Understand tool calling','Learn MCP and A2A basics'],'https://jprime.io/agenda/270',now(),now()),
('00000000-0000-0000-0000-000000000316',287,'Visualizing the Java Concurrency API','Christian Heitzmann','See Java concurrency concepts such as real and virtual threads, locks, executors, and fork/join visualized through a custom live-code application.','Hall B','2026-06-04 11:05','2026-06-04 11:55',ARRAY['Java','Concurrency','Virtual Threads'],'INTERMEDIATE',false,'Technical session','Java developers learning or teaching concurrency concepts.',ARRAY['Visualize concurrency primitives','Understand thread abstractions','Compare real and virtual threads'],'https://jprime.io/agenda/287',now(),now()),
('00000000-0000-0000-0000-000000000317',273,'Java and GenAI: from basics to enterprise','Panche Chavkovski','Move beyond Spring AI hello-world demos toward controlled, observable, secure, identity-aware, human-in-the-loop GenAI implementations.','Hall A','2026-06-04 13:00','2026-06-04 13:50',ARRAY['Java','Spring','AI Agents','Security'],'INTERMEDIATE',false,'Technical session','Java developers building enterprise GenAI features.',ARRAY['Control AI execution flow','Add security and identity','Design HITL workflows'],'https://jprime.io/agenda/273',now(),now()),
('00000000-0000-0000-0000-000000000318',288,'My code is faster than yours... let me prove it to you!','François Martin','A practical introduction to proving Java performance claims with microbenchmarking and JMH through a code-review story.','Hall B','2026-06-04 13:00','2026-06-04 13:50',ARRAY['Java','JVM Performance','Testing'],'ADVANCED',false,'Technical session','Developers who need to prove performance claims with data.',ARRAY['Use JMH microbenchmarks','Avoid performance guesswork','Compare implementation trade-offs'],'https://jprime.io/agenda/288',now(),now()),
('00000000-0000-0000-0000-000000000319',275,'From JVM to GPUs: Building GPU-Accelerated AI Libraries in Java with TornadoVM','Thanos Stratikopoulos','Lessons from building GPU-accelerated AI libraries in Java with TornadoVM, GPULlama3.java, quantized data types, Quarkus, and LangChain4J.','Hall A','2026-06-04 14:05','2026-06-04 14:55',ARRAY['Java','AI Agents','Performance','GPU'],'INTERMEDIATE',false,'Technical session','Java developers interested in high-performance AI workloads.',ARRAY['Understand TornadoVM offloading','Learn Java GPU AI patterns','Connect JVM AI to accelerators'],'https://jprime.io/agenda/275',now(),now()),
('00000000-0000-0000-0000-000000000320',289,'Stream Gatherers - let''s get to know each other better !','Marcin Chrost','Explore Stream Gatherers from JDK 24, including built-in gatherers and custom gatherers for more flexible stream processing.','Hall B','2026-06-04 14:05','2026-06-04 14:55',ARRAY['Java','Modern Java','Streams'],'INTERMEDIATE',false,'Technical session','Java developers modernizing stream-processing code.',ARRAY['Understand Stream Gatherers','Use built-in gatherers','Create custom gatherers'],'https://jprime.io/agenda/289',now(),now()),
('00000000-0000-0000-0000-000000000321',278,'Virtual Threads Are Not Async I/O: Lessons from Operating System Archaeology','Ivan Yonkov','Demystify async programming by tracing operating-system I/O concepts and what the JVM can and cannot change under the hood.','Hall A','2026-06-04 15:20','2026-06-04 16:10',ARRAY['Java','Virtual Threads','Architecture'],'ADVANCED',false,'Technical session','Experienced developers making architecture decisions around async and virtual threads.',ARRAY['Understand async I/O roots','Avoid treating async as magic','Make better JVM architecture decisions'],'https://jprime.io/agenda/278',now(),now()),
('00000000-0000-0000-0000-000000000322',290,'Spring Data Redis Beyond Key-Value','Viktoriya Kutsarova','See how Spring Data Redis is evolving beyond key-value access into advanced data structures, richer querying, Redis JSON, and smoother Spring integration.','Hall B','2026-06-04 15:20','2026-06-04 16:10',ARRAY['Spring','Databases','Redis'],'INTERMEDIATE',false,'Technical session','Spring developers using Redis in modern applications.',ARRAY['Understand Redis integration trends','Explore richer Redis querying','Apply Spring-native Redis capabilities'],'https://jprime.io/agenda/290',now(),now()),
('00000000-0000-0000-0000-000000000323',280,'Beyond the LLM API - What Developers Actually Need to Know About ML','Milen Dyankov','Bridge the gap between prompt engineering and data science by explaining models as software artifacts, inference runtimes, and architectural fundamentals of AI.','Hall A','2026-06-04 16:25','2026-06-04 17:15',ARRAY['AI Agents','Architecture','Machine Learning'],'INTERMEDIATE',false,'Technical session','Software developers integrating AI beyond black-box APIs.',ARRAY['Understand models as artifacts','Talk effectively with data scientists','Move beyond API wrappers'],'https://jprime.io/agenda/280',now(),now()),
('00000000-0000-0000-0000-000000000324',293,'Consistency and Coordination Patterns in Event-Driven Architecture','Emanuel Trandafir','Tackle dual writes, Sagas, orchestration vs choreography, Transactional Outbox and Inbox, and idempotent consumers for reliable event-driven systems.','Hall B','2026-06-04 16:25','2026-06-04 17:15',ARRAY['Architecture','Microservices','Distributed Systems'],'INTERMEDIATE',false,'Technical session','Backend engineers and architects building event-driven microservices.',ARRAY['Handle duplicate and lost messages','Choose coordination patterns','Apply Outbox and Inbox patterns'],'https://jprime.io/agenda/293',now(),now()),
('00000000-0000-0000-0000-000000000325',283,'Agentic Coding - Patterns and Anti-Patterns','Victor Rentea','A field report on professional AI-first software engineering practices: context engineering, MCP tools, agentic design, AI SecOps, and architecture observability.','Hall A','2026-06-04 17:30','2026-06-04 18:20',ARRAY['AI Agents','Developer Productivity','Architecture'],'BEGINNER',true,'Keynote-style session','Developers adapting their engineering practice to AI-first workflows.',ARRAY['Recognize AI coding patterns','Avoid common anti-patterns','Prepare for AI-augmented engineering'],'https://jprime.io/agenda/283',now(),now())
ON CONFLICT (id) DO UPDATE SET
    official_id = EXCLUDED.official_id,
    title = EXCLUDED.title,
    speaker = EXCLUDED.speaker,
    description = EXCLUDED.description,
    hall = EXCLUDED.hall,
    start_time = EXCLUDED.start_time,
    end_time = EXCLUDED.end_time,
    tags = EXCLUDED.tags,
    talk_level = EXCLUDED.talk_level,
    beginner_friendly = EXCLUDED.beginner_friendly,
    format = EXCLUDED.format,
    audience = EXCLUDED.audience,
    takeaways = EXCLUDED.takeaways,
    source_url = EXCLUDED.source_url,
    updated_at = now();
